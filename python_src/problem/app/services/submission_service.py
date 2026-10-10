import asyncio
import time
import uuid
from datetime import datetime
from starlette.concurrency import run_in_threadpool

from app.models.problem import Submission, SubmissionStatus
from app.schemas.problem_schema import SubmissionResponse
from app.core import cache, progress, sqs

from app.schemas.problem_schema import ProblemDTO, TestCaseDTO
from app.models.problem import Problem,TestCase

from app.services.cache_service import CacheService

class SubmissionService:
    def __init__(self, db):
        self.db = db

    async def submit_code(self, data, user_id):
        sub_id = str(uuid.uuid4())
        
        # 1. Update Redis
        cache.set_cache(sub_id, SubmissionStatus.IN_PROGRESS.value)
        progress.write_queued(sub_id)
        
        # 2. Save to DB
        new_sub = Submission(
            submission_id=sub_id,
            user_id=user_id,
            problem_id=data.problemId,
            code=data.code,
            language=data.language,
            status=SubmissionStatus.IN_PROGRESS,
            attempt_id=data.attemptId,
        )
        self.db.add(new_sub)
        self.db.commit()

        # 3. Send to SQS
        sqs.send_to_queue({
            "submissionId": sub_id,
            "userId": user_id,
            "code": data.code,
            "language": data.language,
            "problemId": data.problemId
        })
        return sub_id

    async def long_poll_submission(self, sub_id: str, user):
        """The submission, once judged (or after 10s). None when it does not exist or is not the
        caller's; admins may read any submission."""
        submission = self.db.query(Submission).filter(Submission.submission_id == sub_id).first()
        if not submission or (submission.user_id != user.id and not user.is_admin):
            return None

        # The database only allows a handful of connections in total, and the session would otherwise
        # keep one checked out for the whole wait. Ending the transaction hands it back; the wait
        # below only talks to Redis, and the final query opens a fresh connection.
        self.db.rollback()

        max_wait = 10  # seconds
        waited = 0
        while waited < max_wait:
            status = cache.get_cache(sub_id)
            if status != SubmissionStatus.IN_PROGRESS.value:
                break
            await asyncio.sleep(1)
            waited += 1

        return self.db.query(Submission).filter(Submission.submission_id == sub_id).first()

    # --- live progress (long poll with a version cursor) ---

    POLL_INTERVAL_SECONDS = 0.25

    def _owned_submission_info(self, sub_id: str, user):
        """(language, status) of the caller's submission, or None. Ends the transaction so no database
        connection is held while the caller waits."""
        row = self.db.query(Submission).filter(Submission.submission_id == sub_id).first()
        info = None
        if row and (row.user_id == user.id or user.is_admin):
            info = {"language": row.language, "status": row.status}
        self.db.rollback()
        return info

    def _load_submission(self, sub_id: str):
        row = self.db.query(Submission).filter(Submission.submission_id == sub_id).first()
        result = SubmissionResponse.model_validate(row).model_dump(mode="json") if row else None
        status = row.status.value if row and row.status else None
        self.db.rollback()
        return status, result

    async def progress(self, sub_id: str, user, since: int, wait: int):
        """Waits (up to `wait` seconds) for progress newer than version `since`, then returns the latest
        state. None when the submission does not exist or is not the caller's.

        Redis and database calls run in worker threads so a slow call never stalls the event loop that is
        serving other waiting clients."""
        info = await run_in_threadpool(self._owned_submission_info, sub_id, user)
        if info is None:
            return None
        language = info["language"]
        deadline = time.monotonic() + wait

        async def finished(raw):
            """The final answer: the database is the source of truth for the verdict."""
            status, result = await run_in_threadpool(self._load_submission, sub_id)
            record = dict(raw) if raw else {"v": since + 1}
            if record.get("stage") not in progress.TERMINAL_STAGES:
                record["stage"] = "DONE"  # e.g. the engine predates progress and only set the plain status flag
            body = progress.describe(record, language)
            body.update(changed=True, status=status, result=result, submissionId=sub_id)
            return body

        if info["status"] != SubmissionStatus.IN_PROGRESS:
            return await finished(await asyncio.to_thread(progress.read, sub_id))

        while True:
            raw = await asyncio.to_thread(progress.read, sub_id)
            if raw is None:
                # no progress record (expired, or the engine predates progress): use the plain status flag
                legacy = await asyncio.to_thread(cache.get_cache, sub_id)
                if legacy and legacy != SubmissionStatus.IN_PROGRESS.value:
                    return await finished(None)
            elif raw.get("stage") in progress.TERMINAL_STAGES:
                return await finished(raw)
            elif int(raw.get("v") or 0) > since:
                body = progress.describe(raw, language)
                body.update(changed=True, status=SubmissionStatus.IN_PROGRESS.value, submissionId=sub_id)
                return body

            if time.monotonic() >= deadline:
                body = progress.describe(raw or {"v": since, "stage": "QUEUED"}, language)
                body.update(changed=False, status=SubmissionStatus.IN_PROGRESS.value, submissionId=sub_id)
                return body
            await asyncio.sleep(self.POLL_INTERVAL_SECONDS)

    def get_submissions_by_user_and_problem(self, user_id: int, problem_id: int) -> list[Submission]:
        """Equivalent to getSubmissionByIdAndProblem in Java."""
        return (
            self.db.query(Submission)
            .filter(Submission.user_id == user_id)
            .filter(Submission.problem_id == problem_id)
            .order_by(Submission.submitted_at.desc()) # Added ordering for better UX
            .all()
        )
    
    def add_problem(self, problem_dto: ProblemDTO) -> Problem:
        # 1. Initialize the Problem Model (Matches your Java Entity)
        db_problem = Problem(
            title=problem_dto.title,
            description=problem_dto.description,
            input_description=problem_dto.inputDescription,
            output_description=problem_dto.outputDescription,
            constraints=problem_dto.constraints,
            difficulty=problem_dto.difficulty,
            tags=problem_dto.tags,
            time_limit_ms=problem_dto.timeLimitMs,
            memory_limit_mb=problem_dto.memoryLimitMb
        )

        # 2. Handle nested TestCases (The 'forEach' logic)
        if problem_dto.testCases:
            # In SQLAlchemy, adding items to the relationship list 
            # is equivalent to testCase.setProblem(problem)
            db_problem.test_cases = [
                TestCase(
                    input=tc.input,
                    output=tc.output,
                    is_sample=tc.isSample
                ) for tc in problem_dto.testCases
            ]

        try:
            # 3. Save Problem and all TestCases (CascadeType.ALL)
            self.db.add(db_problem)
            self.db.commit()
            self.db.refresh(db_problem)

            # 4. Clear Cache Keys (Matches Java cacheService.deleteKey)
            CacheService.delete("all_problems_summary")
            CacheService.delete(self.PROBLEM_COUNT_KEY)
            
            return db_problem
        except Exception as e:
            self.db.rollback()
            print(f"Error adding problem: {e}")
            raise e