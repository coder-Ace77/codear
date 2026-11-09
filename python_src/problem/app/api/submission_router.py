from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.services.submission_service import SubmissionService
from app.core.auth import CurrentUser, get_current_user
from app.schemas.problem_schema import SubmissionResponse

router = APIRouter(prefix="/api/v1/problem/submissions")

@router.get("/subuser/{problemId}", response_model=List[SubmissionResponse])
async def get_user_submissions(
    problemId: int,
    user: CurrentUser = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Always the caller's own submissions: the user id comes from the verified token.
    return SubmissionService(db).get_submissions_by_user_and_problem(user.id, problemId)
