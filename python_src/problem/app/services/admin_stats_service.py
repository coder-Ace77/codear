import datetime
import logging

from sqlalchemy import case, func
from sqlalchemy.orm import Session

from app.core import sqs
from app.core.rate_limit import HITS_KEY
from app.database import redis_client
from app.models.problem import Problem, Submission, SubmissionStatus, User

logger = logging.getLogger(__name__)

# A submission still IN_PROGRESS after this long is probably stuck rather than queued.
STALE_AFTER_SECONDS = 120


def _queue_depth(label: str, url):
    entry = {"name": label, "configured": bool(url)}
    if not url:
        return entry
    try:
        attrs = sqs.sqs_client.get_queue_attributes(
            QueueUrl=url,
            AttributeNames=[
                "ApproximateNumberOfMessages",
                "ApproximateNumberOfMessagesNotVisible",
                "ApproximateNumberOfMessagesDelayed",
            ],
        )["Attributes"]
        entry.update(
            waiting=int(attrs.get("ApproximateNumberOfMessages", 0)),
            inFlight=int(attrs.get("ApproximateNumberOfMessagesNotVisible", 0)),
            delayed=int(attrs.get("ApproximateNumberOfMessagesDelayed", 0)),
        )
    except Exception as e:
        logger.warning("could not read queue %s: %s", label, e)
        entry["error"] = "Could not read queue depth"
    return entry


def _rate_limit_hits(now: datetime.datetime, minutes: int = 5) -> int:
    try:
        current = int(now.replace(tzinfo=datetime.timezone.utc).timestamp() // 60)
        values = redis_client.mget([HITS_KEY.format(minute=current - i) for i in range(minutes)])
        return sum(int(v) for v in values if v)
    except Exception:
        return 0


class AdminStatsService:
    def __init__(self, db: Session):
        self.db = db

    def stats(self) -> dict:
        now = datetime.datetime.utcnow()

        def within(seconds: int):
            return func.coalesce(
                func.sum(case((Submission.submitted_at >= now - datetime.timedelta(seconds=seconds), 1), else_=0)), 0
            )

        windows = self.db.query(
            func.count(Submission.id), within(60), within(300), within(3600), within(86400)
        ).one()

        in_progress = self.db.query(func.count(Submission.id), func.min(Submission.submitted_at)).filter(
            Submission.status == SubmissionStatus.IN_PROGRESS
        ).one()
        oldest = int((now - in_progress[1]).total_seconds()) if in_progress[1] else None

        by_status = (
            self.db.query(Submission.status, func.count(Submission.id))
            .filter(Submission.submitted_at >= now - datetime.timedelta(days=1))
            .group_by(Submission.status)
            .all()
        )

        avg_ms = (
            self.db.query(func.avg(Submission.time_taken_ms))
            .filter(Submission.submitted_at >= now - datetime.timedelta(hours=1), Submission.time_taken_ms.isnot(None))
            .scalar()
        )

        top = (
            self.db.query(User.username, Submission.user_id, func.count(Submission.id).label("n"))
            .select_from(Submission)
            .outerjoin(User, User.id == Submission.user_id)
            .filter(Submission.submitted_at >= now - datetime.timedelta(hours=1))
            .group_by(User.username, Submission.user_id)
            .order_by(func.count(Submission.id).desc())
            .limit(5)
            .all()
        )

        return {
            "generatedAt": now.isoformat() + "Z",
            "queues": [
                _queue_depth("submissions", sqs.DEFAULT_QUEUE_URL),
                _queue_depth("test runs", sqs.TEST_QUEUE_URL),
            ],
            "submissions": {
                "total": int(windows[0]),
                "lastMinute": int(windows[1]),
                "last5Minutes": int(windows[2]),
                "lastHour": int(windows[3]),
                "last24Hours": int(windows[4]),
                "perSecondLastMinute": round(int(windows[1]) / 60, 2),
                "inProgress": int(in_progress[0]),
                "oldestInProgressSeconds": oldest,
                "stuck": oldest is not None and oldest > STALE_AFTER_SECONDS,
                "byStatusLast24Hours": {s.value: int(n) for s, n in by_status if s},
                "avgJudgeMsLastHour": int(avg_ms) if avg_ms is not None else None,
            },
            "topSubmittersLastHour": [
                {"userId": uid, "username": name or f"user {uid}", "submissions": int(n)} for name, uid, n in top
            ],
            "rateLimit": {"blockedLast5Minutes": _rate_limit_hits(now)},
        }

    def recent_submissions(self, limit: int) -> list:
        now = datetime.datetime.utcnow()
        rows = (
            self.db.query(Submission, User.username, Problem.title)
            .outerjoin(User, User.id == Submission.user_id)
            .outerjoin(Problem, Problem.id == Submission.problem_id)
            .order_by(Submission.submitted_at.desc())
            .limit(limit)
            .all()
        )
        # Deliberately no code, result text or logs: this view is for operations, not for reading solutions.
        return [
            {
                "submissionId": s.submission_id,
                "userId": s.user_id,
                "username": username or f"user {s.user_id}",
                "problemId": s.problem_id,
                "problemTitle": title,
                "language": s.language,
                "status": s.status.value if s.status else None,
                "passedTests": s.passed_tests,
                "totalTests": s.total_tests,
                "timeTakenMs": s.time_taken_ms,
                "submittedAt": s.submitted_at.isoformat() + "Z" if s.submitted_at else None,
                "ageSeconds": int((now - s.submitted_at).total_seconds()) if s.submitted_at else None,
            }
            for s, username, title in rows
        ]
