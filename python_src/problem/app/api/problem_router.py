from fastapi import APIRouter, Depends, Query, Header, HTTPException
from typing import List, Optional
from app.database import get_db
from app.core.auth import CurrentUser, get_current_user, require_admin
from app.core.rate_limit import rate_limited_submitter
from app.schemas.search_schema import SearchQuery, parse_search_query
from app.services.problem_service import ProblemService
from sqlalchemy.orm import Session
from app.services.submission_service import SubmissionService
from app.services.test_service import TestService
from app.schemas.problem_schema import  ProblemDTO, ProblemSendDTO, ProblemsMetaData, ProblemSummaryDTO, CodeRequest, SubmissionResponse
import uuid
from app.schemas.problem_schema import TestDTO
from app.core.sqs import send_to_queue, TEST_QUEUE_URL
from app.services.cache_service import CacheService
import time
import logging
from functools import wraps

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/problem")

def profile_time(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        end_time = time.time()
        logger.info(f"Execution time for {func.__name__}: {end_time - start_time:.4f} seconds")
        return result
    return wrapper

@router.get("/search")
def search(query: SearchQuery = Depends(parse_search_query), db=Depends(get_db)):
    """Problems matching the words (in the title, statement or tags), the difficulty and the tags, in the order asked
    for. See schemas/search_schema.py for every parameter."""
    return ProblemService(db).search_problems(query)


@router.get("/tags")
def tags(db=Depends(get_db)):
    """Every tag with the number of problems that carry it, most used first."""
    return ProblemService(db).tag_counts()


@router.post("/test")
async def run_test_case(test_data: TestDTO, user: CurrentUser = Depends(get_current_user)):
    submission_id = str(uuid.uuid4())
    test_data.submissionId = submission_id
    test_data.status = "IN_PROGRESS"
    test_data.userId = user.id  # always the caller, whatever the body says
    send_to_queue(test_data.model_dump(), queue_url=TEST_QUEUE_URL)
    CacheService.set_object(submission_id, test_data.model_dump(), expire_seconds=600)

    return {
        "message": "Test in queue",
        "submissionId": submission_id
    }

@router.post("/submit")
async def submit(
    data: CodeRequest,
    user: CurrentUser = Depends(rate_limited_submitter),
    db=Depends(get_db)
):
    if data.attemptId is not None:
        TestService(db).check_can_submit(data.attemptId, data.problemId, user)
    service = SubmissionService(db)
    res = await service.submit_code(data, user.id)
    return {"message": "Code submitted successfully", "submissionId": res}


@router.get("/submissions/{submissionId}", response_model=SubmissionResponse)
async def get_status(
    submissionId: str,
    user: CurrentUser = Depends(get_current_user),
    db=Depends(get_db),
):
    service = SubmissionService(db)
    submission = await service.long_poll_submission(submissionId, user)
    if not submission:
        # Someone else's submission looks exactly like one that does not exist.
        raise HTTPException(status_code=404, detail="Submission not found")
    return submission

@router.get("/problem/{id}", response_model=ProblemSendDTO)
def get_problem_by_id(id: int, db: Session = Depends(get_db)):
    service = ProblemService(db)
    problem = service.get_problem_by_id(id)
    if not problem:
        raise HTTPException(status_code=404, detail=f"Problem with id {id} not found")
    return problem

@router.get("/problemCntAndTags", response_model=ProblemsMetaData)
def get_problem_cnt_and_tags(db: Session = Depends(get_db)):
    service = ProblemService(db)
    return {
        "count": service.get_problem_cnt(),
        "tags": service.get_tags_for_problem()
    }

@router.get("/recent", response_model=List[ProblemSummaryDTO])
def get_recent_problems(
    user: CurrentUser = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    service = ProblemService(db)
    return service.get_problem_summary_recent(user.id)

@router.get("/problems", response_model=List[ProblemSummaryDTO])
def get_all_problems(db: Session = Depends(get_db)):
    service = ProblemService(db)
    return service.get_all_problems()

@router.post("/addproblem", response_model=ProblemDTO)
async def create_problem(
    problem: ProblemDTO,
    _admin: CurrentUser = Depends(require_admin),
    db: Session = Depends(get_db),
):
    service = ProblemService(db)
    saved_problem = service.add_problem(problem)
    return saved_problem

@router.delete("/{id}")
async def delete_problem(
    id: int,
    _admin: CurrentUser = Depends(require_admin),
    db: Session = Depends(get_db),
):
    service = ProblemService(db)
    success = service.delete_problem(id)
    if not success:
        raise HTTPException(status_code=404, detail=f"Problem with id {id} not found")
    return {"message": "Problem deleted successfully"}