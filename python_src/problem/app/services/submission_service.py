import asyncio
import uuid
from datetime import datetime
from app.models.problem import Submission, SubmissionStatus
from app.core import cache, sqs

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
        
        # 2. Save to DB
        new_sub = Submission(
            submission_id=sub_id,
            user_id=user_id,
            problem_id=data.problemId,
            code=data.code,
            language=data.language,
            status=SubmissionStatus.IN_PROGRESS
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

    async def long_poll_submission(self, sub_id: str):
        max_wait = 10  # seconds
        waited = 0
        
        while waited < max_wait:
            status = cache.get_cache(sub_id)
            if status != SubmissionStatus.IN_PROGRESS.value:
                break
            await asyncio.sleep(1)
            waited += 1
            
        submission = self.db.query(Submission).filter(Submission.submission_id == sub_id).first()
        return submission
    
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