from fastapi import APIRouter, Depends, Query, Header, HTTPException
from typing import List, Optional
from app.database import get_db
from app.core import security
from app.services.problem_service import ProblemService
from sqlalchemy.orm import Session
from app.services.submission_service import SubmissionService
from app.schemas.problem_schema import  ProblemDTO, ProblemSendDTO, ProblemsMetaData, ProblemSummaryDTO, CodeRequest
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

@profile_time
@router.get("/search")
async def search(
    search: Optional[str] = None,
    difficulty: Optional[str] = None,
    tags: Optional[List[str]] = Query(None),
    page: int = 0,
    size: int = 10,
    db=Depends(get_db)
):
    service = ProblemService(db)
    content = service.search_problems(search, difficulty, tags, page, size)
    total = service.count_filtered_problems(search, difficulty, tags)
    return {
        "content": content,
        "totalCount": total,
        "totalPages": (total + size - 1) // size
    }

@router.post("/test")
async def run_test_case(test_data: TestDTO):
    submission_id = str(uuid.uuid4())
    test_data.submissionId = submission_id
    test_data.status = "IN_PROGRESS"
    send_to_queue(test_data.model_dump(), queue_url=TEST_QUEUE_URL)
    CacheService.set_object(submission_id, test_data.model_dump(), expire_seconds=600)
    
    return {
        "message": "Test in queue",
        "submissionId": submission_id
    }

@router.post("/submit")
async def submit(
    data: CodeRequest, 
    authorization: str = Header(...), 
    db=Depends(get_db)
):
    user_id = security.extract_user_id(authorization.split(" ")[1])
    service = SubmissionService(db)
    res = await service.submit_code(data, int(user_id))
    return {"message": "Code submitted successfully", "submissionId": res}


@router.get("/submissions/{submissionId}")
async def get_status(submissionId: str, db=Depends(get_db)):
    service = SubmissionService(db)
    return await service.long_poll_submission(submissionId)

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
    authorization: str = Header(...), 
    db: Session = Depends(get_db)
):
    token = authorization.split(" ")[1]
    user_id = security.extract_user_id(token)
    
    if not user_id:
        raise HTTPException(status_code=401, detail="Unauthorized")
        
    service = ProblemService(db)
    return service.get_problem_summary_recent(user_id)

@router.get("/problems", response_model=List[ProblemSummaryDTO])
def get_all_problems(db: Session = Depends(get_db)):
    service = ProblemService(db)
    return service.get_all_problems()

@router.post("/addproblem", response_model=ProblemDTO)
async def create_problem(problem: ProblemDTO, db: Session = Depends(get_db)):
    service = ProblemService(db)
    saved_problem = service.add_problem(problem)
    return saved_problem

@router.delete("/{id}")
async def delete_problem(id: int, db: Session = Depends(get_db)):
    service = ProblemService(db)
    success = service.delete_problem(id)
    if not success:
        raise HTTPException(status_code=404, detail=f"Problem with id {id} not found")
    return {"message": "Problem deleted successfully"}