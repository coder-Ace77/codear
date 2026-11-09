import datetime
import random
import secrets
from typing import List, Optional

from fastapi import HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core.auth import CurrentUser
from app.models.problem import Assessment, Problem, Submission, SubmissionStatus, AssessmentAttempt, AssessmentInvite, User
from app.schemas.assessment_schema import (
    AttemptOut,
    ActiveAttempt,
    AttemptProblem,
    AttemptResult,
    CreatedAssessment,
    CreateAssessmentRequest,
    InviteOut,
    InviteSettings,
    PublicAssessment,
    AssessmentHistory,
    ProblemResult,
    SelectionMode,
    AssessmentDetail,
    AssessmentSummary,
    Visibility,
)

_random = random.SystemRandom()


def _now() -> datetime.datetime:
    return datetime.datetime.utcnow()


def _utc(moment: Optional[datetime.datetime]) -> Optional[datetime.datetime]:
    """Timestamps are stored as naive UTC; send them with the zone so browsers do not read them as local time."""
    return moment.replace(tzinfo=datetime.timezone.utc) if moment else None


def _naive_utc(moment: Optional[datetime.datetime]) -> Optional[datetime.datetime]:
    if moment is not None and moment.tzinfo is not None:
        return moment.astimezone(datetime.timezone.utc).replace(tzinfo=None)
    return moment


def _is_over(attempt: AssessmentAttempt) -> bool:
    return attempt.finished_at is not None or (attempt.expires_at is not None and attempt.expires_at <= _now())


def draw_problems(candidates: List[tuple], slots: Optional[List[str]], count: int) -> List[int]:
    """Picks `count` distinct problem ids at random from `candidates` ((id, difficulty) pairs).
    `slots` says which difficulty each position must have (ANY, EASY, MEDIUM, HARD); None means all ANY.
    Raises ValueError when the candidates cannot fill every slot."""
    slots = slots or ["ANY"] * count
    by_level = {}
    for pid, level in candidates:
        by_level.setdefault((level or "").strip().upper(), []).append(pid)

    chosen: List[Optional[int]] = [None] * len(slots)
    used = set()
    # fixed difficulties first, so the open slots only draw from what is left over
    for i, wanted in enumerate(slots):
        if wanted == "ANY":
            continue
        pool = [pid for pid in by_level.get(wanted, []) if pid not in used]
        if not pool:
            raise ValueError(f"not enough {wanted.lower()} problems")
        chosen[i] = _random.choice(pool)
        used.add(chosen[i])
    for i, wanted in enumerate(slots):
        if wanted != "ANY":
            continue
        pool = [pid for pid, _ in candidates if pid not in used]
        if not pool:
            raise ValueError("not enough problems")
        chosen[i] = _random.choice(pool)
        used.add(chosen[i])
    return chosen


class AssessmentService:
    def __init__(self, db: Session):
        self.db = db

    # ---------------------------------------------------------------- admin

    def create(self, req: CreateAssessmentRequest, admin: CurrentUser) -> CreatedAssessment:
        slots = [d.value for d in req.slotDifficulties] if req.slotDifficulties else None
        if req.selectionMode == SelectionMode.POOL:
            found = {row[0] for row in self.db.query(Problem.id).filter(Problem.id.in_(req.poolProblemIds)).all()}
            missing = [pid for pid in req.poolProblemIds if pid not in found]
            if missing:
                raise HTTPException(status_code=400, detail=f"Unknown problem ids: {missing}")
        try:
            # the same draw a participant will get, so an assessment that could not be started is refused now
            draw_problems(self._candidates(req.selectionMode.value, req.poolProblemIds), slots, req.problemCount)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=f"This assessment cannot be built: {e}")

        assessment = Assessment(
            title=req.title,
            description=req.description,
            visibility=req.visibility.value,
            selection_mode=req.selectionMode.value,
            pool_problem_ids=req.poolProblemIds,
            problem_count=req.problemCount,
            slot_difficulties=slots,
            duration_minutes=req.durationMinutes,
            allow_retakes=req.multipleAttempts,
            created_by=admin.id,
        )
        self.db.add(assessment)
        self.db.flush()

        unknown = self._add_invites(assessment, req.invites)
        self.db.commit()
        self.db.refresh(assessment)
        return CreatedAssessment(assessment=self._detail(assessment), unknownUsernames=unknown)

    def add_invites(self, assessment_id: int, settings: InviteSettings) -> CreatedAssessment:
        assessment = self._get_assessment(assessment_id)
        if assessment.visibility != Visibility.PRIVATE.value:
            raise HTTPException(status_code=400, detail="Only private assessments have invite links")
        unknown = self._add_invites(assessment, settings)
        self.db.commit()
        self.db.refresh(assessment)
        return CreatedAssessment(assessment=self._detail(assessment), unknownUsernames=unknown)

    def _add_invites(self, assessment: Assessment, settings: InviteSettings) -> List[str]:
        if not settings.usernames:
            return []
        lowered = [name.lower() for name in settings.usernames]
        users = self.db.query(User).filter(func.lower(User.username).in_(lowered)).all()
        by_name = {u.username.lower(): u for u in users}
        expires = _naive_utc(settings.expiresAt)
        single_use = settings.singleUse if settings.singleUse is not None else not assessment.allow_retakes
        unknown = []
        for name in settings.usernames:
            user = by_name.get(name.lower())
            if not user:
                unknown.append(name)
                continue
            self.db.add(
                AssessmentInvite(
                    assessment_id=assessment.id,
                    user_id=user.id,
                    token=secrets.token_urlsafe(24),
                    single_use=single_use,
                    expires_at=expires,
                )
            )
        return unknown

    def list_assessments(self) -> List[AssessmentSummary]:
        assessments = self.db.query(Assessment).order_by(Assessment.created_at.desc()).all()
        attempts = dict(self.db.query(AssessmentAttempt.assessment_id, func.count()).group_by(AssessmentAttempt.assessment_id).all())
        invites = dict(self.db.query(AssessmentInvite.assessment_id, func.count()).group_by(AssessmentInvite.assessment_id).all())
        return [self._summary(t, attempts.get(t.id, 0), invites.get(t.id, 0)) for t in assessments]

    def get_detail(self, assessment_id: int) -> AssessmentDetail:
        return self._detail(self._get_assessment(assessment_id))

    def set_active(self, assessment_id: int, active: bool) -> AssessmentDetail:
        assessment = self._get_assessment(assessment_id)
        assessment.is_active = active
        self.db.commit()
        return self._detail(assessment)

    def revoke_invite(self, assessment_id: int, invite_id: int) -> None:
        invite = self.db.query(AssessmentInvite).filter(AssessmentInvite.id == invite_id, AssessmentInvite.assessment_id == assessment_id).first()
        if not invite:
            raise HTTPException(status_code=404, detail="Invite not found")
        if invite.revoked_at is None:
            invite.revoked_at = _now()
            self.db.commit()

    def delete(self, assessment_id: int) -> None:
        self.db.delete(self._get_assessment(assessment_id))
        self.db.commit()

    def _get_assessment(self, assessment_id: int) -> Assessment:
        assessment = self.db.query(Assessment).filter(Assessment.id == assessment_id).first()
        if not assessment:
            raise HTTPException(status_code=404, detail="Assessment not found")
        return assessment

    def _summary(self, assessment: Assessment, attempts: int, invites: int) -> AssessmentSummary:
        return AssessmentSummary(
            id=assessment.id,
            title=assessment.title,
            description=assessment.description,
            visibility=assessment.visibility,
            selectionMode=assessment.selection_mode,
            problemCount=assessment.problem_count,
            slotDifficulties=assessment.slot_difficulties,
            durationMinutes=assessment.duration_minutes,
            multipleAttempts=bool(assessment.allow_retakes),
            isActive=assessment.is_active,
            createdAt=_utc(assessment.created_at),
            attempts=attempts,
            invites=invites,
        )

    def _detail(self, assessment: Assessment) -> AssessmentDetail:
        invites = self.db.query(AssessmentInvite).filter(AssessmentInvite.assessment_id == assessment.id).order_by(AssessmentInvite.id).all()
        names = {
            u.id: u.username
            for u in self.db.query(User).filter(User.id.in_([i.user_id for i in invites])).all()
        } if invites else {}
        used = {
            row[0] for row in self.db.query(AssessmentAttempt.invite_id).filter(AssessmentAttempt.assessment_id == assessment.id).all()
        }
        attempts = self.db.query(func.count(AssessmentAttempt.id)).filter(AssessmentAttempt.assessment_id == assessment.id).scalar() or 0
        base = self._summary(assessment, attempts, len(invites))
        return AssessmentDetail(
            **base.model_dump(),
            poolProblemIds=assessment.pool_problem_ids,
            inviteList=[
                InviteOut(
                    id=i.id,
                    userId=i.user_id,
                    username=names.get(i.user_id),
                    token=i.token,
                    singleUse=i.single_use,
                    expiresAt=_utc(i.expires_at),
                    revokedAt=_utc(i.revoked_at),
                    path=f"/assessment/{i.token}",
                    used=i.id in used,
                )
                for i in invites
            ],
        )

    # ---------------------------------------------------------- participants

    def list_public(self, user: CurrentUser) -> List[PublicAssessment]:
        assessments = (
            self.db.query(Assessment)
            .filter(Assessment.visibility == Visibility.PUBLIC.value, Assessment.is_active.is_(True))
            .order_by(Assessment.created_at.desc())
            .all()
        )
        latest = {}
        if assessments:
            for attempt in (
                self.db.query(AssessmentAttempt)
                .filter(AssessmentAttempt.user_id == user.id, AssessmentAttempt.assessment_id.in_([t.id for t in assessments]))
                .order_by(AssessmentAttempt.id)
                .all()
            ):
                latest[attempt.assessment_id] = attempt  # ordered by id, so the last one wins
        out = []
        for t in assessments:
            attempt = latest.get(t.id)
            if attempt is None:
                status, attempt_id = "NOT_STARTED", None
            elif not _is_over(attempt):
                status, attempt_id = "IN_PROGRESS", attempt.id
            else:
                status, attempt_id = ("RETAKE" if t.allow_retakes else "DONE"), attempt.id
            out.append(PublicAssessment(**self._summary(t, 0, 0).model_dump(), myStatus=status, myAttemptId=attempt_id))
        return out

    def start_public(self, assessment_id: int, user: CurrentUser) -> AttemptOut:
        # The row lock makes two simultaneous starts by the same user queue up, so the second one
        # sees the attempt the first one created instead of making another.
        assessment = self.db.query(Assessment).filter(Assessment.id == assessment_id).with_for_update().first()
        if not assessment or assessment.visibility != Visibility.PUBLIC.value or not assessment.is_active:
            raise HTTPException(status_code=404, detail="Assessment not found")

        previous = (
            self.db.query(AssessmentAttempt)
            .filter(AssessmentAttempt.assessment_id == assessment.id, AssessmentAttempt.user_id == user.id)
            .order_by(AssessmentAttempt.id.desc())
            .first()
        )
        if previous and not _is_over(previous):
            return self._attempt_out(previous, assessment)
        if previous and not assessment.allow_retakes:
            raise HTTPException(status_code=409, detail="You have already taken this assessment")
        return self._attempt_out(self._begin(assessment, user, invite=None), assessment)

    def start_with_invite(self, token: str, user: CurrentUser) -> AttemptOut:
        invite = self.db.query(AssessmentInvite).filter(AssessmentInvite.token == token).with_for_update().first()
        if not invite:
            raise HTTPException(status_code=404, detail="This assessment link is not valid")
        if invite.user_id != user.id:
            raise HTTPException(status_code=403, detail="This link was issued to a different account")
        assessment = self.db.query(Assessment).filter(Assessment.id == invite.assessment_id).first()
        if (
            not assessment
            or not assessment.is_active
            or invite.revoked_at is not None
            or (invite.expires_at is not None and invite.expires_at <= _now())
        ):
            raise HTTPException(status_code=410, detail="This assessment link is no longer active")

        attempts = (
            self.db.query(AssessmentAttempt).filter(AssessmentAttempt.invite_id == invite.id).order_by(AssessmentAttempt.id.desc()).all()
        )
        # An attempt that is still running can always be picked up again (a refresh, a dropped connection).
        if attempts and not _is_over(attempts[0]):
            return self._attempt_out(attempts[0], assessment)
        if attempts and invite.single_use:
            raise HTTPException(status_code=410, detail="This assessment link has already been used")
        return self._attempt_out(self._begin(assessment, user, invite=invite), assessment)

    def _begin(self, assessment: Assessment, user: CurrentUser, invite: Optional[AssessmentInvite]) -> AssessmentAttempt:
        try:
            drawn = draw_problems(
                self._candidates(assessment.selection_mode, assessment.pool_problem_ids), assessment.slot_difficulties, assessment.problem_count
            )
        except ValueError:
            # problems were deleted (or re-rated) after the assessment was made
            raise HTTPException(status_code=409, detail="This assessment does not have enough problems left")

        titles = dict(self.db.query(Problem.id, Problem.title).filter(Problem.id.in_(drawn)).all())
        started = _now()
        attempt = AssessmentAttempt(
            assessment_id=assessment.id,
            invite_id=invite.id if invite else None,
            user_id=user.id,
            problem_ids=drawn,
            problem_titles=[titles.get(pid, "") for pid in drawn],
            started_at=started,
            expires_at=started + datetime.timedelta(minutes=assessment.duration_minutes) if assessment.duration_minutes else None,
        )
        self.db.add(attempt)
        self.db.commit()
        self.db.refresh(attempt)
        return attempt

    def _candidates(self, selection_mode: str, pool: Optional[List[int]]) -> List[tuple]:
        query = self.db.query(Problem.id, Problem.difficulty)
        if selection_mode == SelectionMode.POOL.value:
            query = query.filter(Problem.id.in_(pool or []))
        return [(pid, level) for pid, level in query.all()]

    def active_attempts(self, user: CurrentUser) -> List[ActiveAttempt]:
        """The caller's attempts that can still be worked on."""
        now = _now()
        attempts = (
            self.db.query(AssessmentAttempt)
            .filter(
                AssessmentAttempt.user_id == user.id,
                AssessmentAttempt.finished_at.is_(None),
                (AssessmentAttempt.expires_at.is_(None)) | (AssessmentAttempt.expires_at > now),
            )
            .order_by(AssessmentAttempt.started_at.desc())
            .all()
        )
        if not attempts:
            return []
        solved = self._solved_at([a.id for a in attempts])
        titles = {
            t.id: t.title
            for t in self.db.query(Assessment).filter(Assessment.id.in_({a.assessment_id for a in attempts})).all()
        }
        return [
            ActiveAttempt(
                attemptId=a.id,
                assessmentId=a.assessment_id,
                title=titles.get(a.assessment_id, "Assessment"),
                startedAt=_utc(a.started_at),
                expiresAt=_utc(a.expires_at),
                secondsRemaining=max(0, int((a.expires_at - now).total_seconds())) if a.expires_at else None,
                solved=len(solved[a.id]),
                total=len(a.problem_ids),
            )
            for a in attempts
        ]

    def get_attempt(self, attempt_id: int, user: CurrentUser) -> AttemptOut:
        attempt = self.db.query(AssessmentAttempt).filter(AssessmentAttempt.id == attempt_id).first()
        # someone else's attempt looks exactly like one that does not exist
        if not attempt or (attempt.user_id != user.id and not user.is_admin):
            raise HTTPException(status_code=404, detail="Attempt not found")
        assessment = self.db.query(Assessment).filter(Assessment.id == attempt.assessment_id).first()
        return self._attempt_out(attempt, assessment)

    def finish(self, attempt_id: int, user: CurrentUser) -> AttemptOut:
        attempt = self.db.query(AssessmentAttempt).filter(AssessmentAttempt.id == attempt_id, AssessmentAttempt.user_id == user.id).first()
        if not attempt:
            raise HTTPException(status_code=404, detail="Attempt not found")
        if attempt.finished_at is None:
            attempt.finished_at = _now()
            self.db.commit()
        assessment = self.db.query(Assessment).filter(Assessment.id == attempt.assessment_id).first()
        return self._attempt_out(attempt, assessment)

    def _attempt_out(self, attempt: AssessmentAttempt, assessment: Assessment) -> AttemptOut:
        rows = {
            p.id: p for p in self.db.query(Problem).filter(Problem.id.in_(attempt.problem_ids)).all()
        }
        solved = self._solved_at([attempt.id])[attempt.id]
        now = _now()
        remaining = None
        if attempt.expires_at is not None:
            remaining = max(0, int((attempt.expires_at - now).total_seconds()))
        ended = min(m for m in (now, attempt.finished_at, attempt.expires_at) if m is not None)
        elapsed = max(0, int((ended - attempt.started_at).total_seconds()))
        return AttemptOut(
            attemptId=attempt.id,
            assessmentId=assessment.id,
            title=assessment.title,
            description=assessment.description,
            startedAt=_utc(attempt.started_at),
            expiresAt=_utc(attempt.expires_at),
            secondsRemaining=remaining,
            secondsElapsed=elapsed,
            finished=_is_over(attempt),
            # deleted problems drop out; the order drawn is kept
            problems=[
                AttemptProblem(id=pid, title=rows[pid].title)
                for pid in attempt.problem_ids
                if pid in rows
            ],
            solved=[pid for pid in attempt.problem_ids if pid in solved],
        )

    # ------------------------------------------------- submissions and results

    def check_can_submit(self, attempt_id: int, problem_id: int, user: CurrentUser) -> None:
        """Raises unless `user` may submit `problem_id` as part of attempt `attempt_id` right now.
        The server clock decides; the countdown in the browser is only a display."""
        attempt = self.db.query(AssessmentAttempt).filter(AssessmentAttempt.id == attempt_id).first()
        if not attempt or attempt.user_id != user.id:
            raise HTTPException(status_code=404, detail="Attempt not found")
        if _is_over(attempt):
            raise HTTPException(status_code=409, detail="This assessment is over, no more submissions")
        if problem_id not in attempt.problem_ids:
            raise HTTPException(status_code=400, detail="That problem is not part of this assessment")

    def _solved_at(self, attempt_ids: List[int]) -> dict:
        """{attempt id: {problem id: when it was first accepted}}"""
        out = {a: {} for a in attempt_ids}
        rows = (
            self.db.query(Submission.attempt_id, Submission.problem_id, func.min(Submission.submitted_at))
            .filter(Submission.attempt_id.in_(attempt_ids), Submission.status == SubmissionStatus.PASSED)
            .group_by(Submission.attempt_id, Submission.problem_id)
            .all()
        )
        for attempt_id, problem_id, at in rows:
            out[attempt_id][problem_id] = at
        return out

    def results(self, assessment_id: int) -> List[AttemptResult]:
        self._get_assessment(assessment_id)
        attempts = self.db.query(AssessmentAttempt).filter(AssessmentAttempt.assessment_id == assessment_id).order_by(AssessmentAttempt.id).all()
        return self._results_for(attempts)

    HISTORY_LIMIT = 200

    def history(self, user_id: int) -> AssessmentHistory:
        """Every assessment attempt of this user that is over, newest first."""
        now = _now()
        attempts = (
            self.db.query(AssessmentAttempt)
            .filter(
                AssessmentAttempt.user_id == user_id,
                (AssessmentAttempt.finished_at.isnot(None)) | (AssessmentAttempt.expires_at <= now),
            )
            .order_by(AssessmentAttempt.started_at.desc())
            .limit(self.HISTORY_LIMIT)
            .all()
        )
        rows = self._results_for(attempts)
        user = self.db.query(User).filter(User.id == user_id).first()
        return AssessmentHistory(
            userId=user_id,
            username=user.username if user else None,
            attemptsTaken=len(rows),
            problemsSolved=sum(r.solvedCount for r in rows),
            problemsGiven=sum(r.total for r in rows),
            averageTimeSeconds=round(sum(r.timeTakenSeconds for r in rows) / len(rows)) if rows else None,
            attempts=rows,
        )

    def history_by_username(self, username: str) -> AssessmentHistory:
        user = self.db.query(User).filter(func.lower(User.username) == username.strip().lower()).first()
        if not user:
            raise HTTPException(status_code=404, detail="No such user")
        return self.history(user.id)

    def _results_for(self, attempts: List[AssessmentAttempt]) -> List[AttemptResult]:
        if not attempts:
            return []
        ids = [a.id for a in attempts]
        solved = self._solved_at(ids)
        counts = {
            (a, p): n
            for a, p, n in self.db.query(Submission.attempt_id, Submission.problem_id, func.count())
            .filter(Submission.attempt_id.in_(ids))
            .group_by(Submission.attempt_id, Submission.problem_id)
            .all()
        }
        names = {u.id: u.username for u in self.db.query(User).filter(User.id.in_({a.user_id for a in attempts})).all()}
        assessments = {t.id: t for t in self.db.query(Assessment).filter(Assessment.id.in_({a.assessment_id for a in attempts})).all()}
        live_titles = {
            p.id: p.title
            for p in self.db.query(Problem).filter(Problem.id.in_({p for a in attempts for p in a.problem_ids})).all()
        }
        now = _now()
        out = []
        for a in attempts:
            snapshot = dict(zip(a.problem_ids, a.problem_titles or []))
            problems = [
                ProblemResult(
                    id=pid,
                    title=snapshot.get(pid) or live_titles.get(pid) or f"Problem {pid} (deleted)",
                    solved=pid in solved[a.id],
                    submissions=counts.get((a.id, pid), 0),
                    solvedAt=_utc(solved[a.id].get(pid)),
                )
                for pid in a.problem_ids
            ]
            if a.finished_at is not None:
                outcome, ended = "FINISHED", a.finished_at
            elif a.expires_at is not None and a.expires_at <= now:
                outcome, ended = "TIME_UP", a.expires_at
            else:
                outcome, ended = "IN_PROGRESS", now
            assessment = assessments.get(a.assessment_id)
            out.append(
                AttemptResult(
                    attemptId=a.id,
                    assessmentId=a.assessment_id,
                    assessmentTitle=assessment.title if assessment else "Assessment",
                    userId=a.user_id,
                    username=names.get(a.user_id),
                    startedAt=_utc(a.started_at),
                    finishedAt=_utc(a.finished_at),
                    expiresAt=_utc(a.expires_at),
                    durationMinutes=assessment.duration_minutes if assessment else None,
                    over=outcome != "IN_PROGRESS",
                    outcome=outcome,
                    timeTakenSeconds=max(0, int((ended - a.started_at).total_seconds())),
                    solvedCount=sum(1 for p in problems if p.solved),
                    total=len(problems),
                    problems=problems,
                )
            )
        return out
