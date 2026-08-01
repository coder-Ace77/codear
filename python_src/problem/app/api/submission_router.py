from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.services.submission_service import SubmissionService
from app.core import security
from app.schemas.problem_schema import SubmissionResponse
from app.services.cache_service import CacheService
from app.schemas.problem_schema import TestDTO
import asyncio

router = APIRouter(prefix="/api/v1/problem/submissions")

@router.get("/subuser/{problemId}", response_model=List[SubmissionResponse])
async def get_user_submissions(
    problemId: int, 
    authorization: str = Header(...), 
    db: Session = Depends(get_db)
):
    # 1. Handle Authorization Header
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")
    
    token = authorization.split(" ")[1]
    
    # 2. Extract user_id using the Security module
    user_id = security.extract_user_id(token)
    if not user_id:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    
    # 3. Fetch data using Service
    service = SubmissionService(db)
    submissions = service.get_submissions_by_user_and_problem(user_id, problemId)
    
    return submissions


@router.get("/test/{submissionId}")
async def test_polling(submissionId: str):
    """
    Equivalent to TestCaseRun.longPollingService in Java.
    Polls Redis for up to 10 seconds to see if the status has changed from IN_PROGRESS.
    """
    max_wait_time = 1  # Total seconds to wait
    poll_interval = 1   # Seconds between each Redis check
    waited = 0

    # Initial fetch
    test_result = CacheService.get_object(submissionId)
    
    if not test_result:
        raise HTTPException(
            status_code=404, 
            detail="Test session not found. It may have expired or never existed."
        )

    # Long Polling Loop
    # We loop while status is IN_PROGRESS and we haven't exceeded max_wait_time
    while test_result.get("status") == "IN_PROGRESS" and waited < max_wait_time:
        await asyncio.sleep(poll_interval)
        waited += poll_interval
        
        # Re-fetch from Redis
        test_result = CacheService.get_object(submissionId)
        if not test_result:
            break # Safety break if object expires during polling

    return test_result