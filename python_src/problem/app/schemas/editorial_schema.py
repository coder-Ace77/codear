from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class EditorialCreateDTO(BaseModel):
    problemId: int
    title: str
    content: str
    
class EditorialDTO(BaseModel):
    id: int
    problemId: int = Field(..., alias="problem_id")
    userId: int = Field(..., alias="user_id")
    username: str
    title: str
    content: str
    isAdmin: bool = Field(..., alias="is_admin")
    upvotes: int
    createdAt: datetime = Field(..., alias="created_at")
    updatedAt: Optional[datetime] = Field(None, alias="updated_at")

    class Config:
        from_attributes = True
        populate_by_name = True
