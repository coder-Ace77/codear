from sqlalchemy.orm import Session
from sqlalchemy import desc, case
from app.models.problem import Editorial
from app.schemas.editorial_schema import EditorialCreateDTO, EditorialDTO
from typing import List

class EditorialService:
    def __init__(self, db: Session):
        self.db = db

    def add_editorial(self, dto: EditorialCreateDTO, user_id: int, username: str) -> Editorial:
        is_admin = (username == "admin")
        
        editorial = Editorial(
            problem_id=dto.problemId,
            user_id=user_id,
            username=username,
            title=dto.title,
            content=dto.content,
            is_admin=is_admin
        )
        
        self.db.add(editorial)
        self.db.commit()
        self.db.refresh(editorial)
        return editorial

    def get_editorials(self, problem_id: int) -> List[dict]:
        # Sort by is_admin descending (True first), then by upvotes descending
        editorials = self.db.query(Editorial).filter(
            Editorial.problem_id == problem_id
        ).order_by(
            desc(Editorial.is_admin),
            desc(Editorial.upvotes)
        ).all()

        return [
            {
                "id": e.id,
                "problemId": e.problem_id,
                "userId": e.user_id,
                "username": e.username,
                "title": e.title,
                "content": e.content,
                "isAdmin": e.is_admin,
                "upvotes": e.upvotes,
                "createdAt": e.created_at,
                "updatedAt": e.updated_at
            }
            for e in editorials
        ]
