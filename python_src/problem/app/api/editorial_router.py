from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.services.editorial_service import EditorialService
from app.schemas.editorial_schema import EditorialCreateDTO, EditorialDTO
from app.core.auth import CurrentUser, get_current_user
from typing import List

router = APIRouter(prefix="/api/v1/problem")

@router.post("/{problemId}/editorial", response_model=EditorialDTO)
def create_editorial(
    problemId: int,
    dto: EditorialCreateDTO,
    user: CurrentUser = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Ensure URL problemId matches body problemId
    dto.problemId = problemId

    service = EditorialService(db)
    return service.add_editorial(
        dto=dto,
        user_id=user.id,
        username=user.username,
        is_admin=user.is_admin,
    )

@router.get("/{problemId}/editorial", response_model=List[EditorialDTO])
def get_editorials(problemId: int, db: Session = Depends(get_db)):
    service = EditorialService(db)
    return service.get_editorials(problemId)
