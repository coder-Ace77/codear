from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.services.editorial_service import EditorialService
from app.schemas.editorial_schema import EditorialCreateDTO, EditorialDTO
from app.core import security
from typing import List

router = APIRouter(prefix="/api/v1/problem")

@router.post("/{problemId}/editorial", response_model=EditorialDTO)
def create_editorial(
    problemId: int,
    dto: EditorialCreateDTO,
    authorization: str = Header(...),
    db: Session = Depends(get_db)
):
    token = authorization.split(" ")[1]
    user_context = security.extract_user_context(token)
    
    if not user_context:
        raise HTTPException(status_code=401, detail="Unauthorized")
        
    # Ensure URL problemId matches body problemId
    dto.problemId = problemId
        
    service = EditorialService(db)
    return service.add_editorial(
        dto=dto, 
        user_id=user_context["id"], 
        username=user_context["username"]
    )

@router.get("/{problemId}/editorial", response_model=List[EditorialDTO])
def get_editorials(problemId: int, db: Session = Depends(get_db)):
    service = EditorialService(db)
    return service.get_editorials(problemId)
