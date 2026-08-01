from pydantic import BaseModel, EmailStr
from typing import Optional

class RegisterDTO(BaseModel):
    username: str
    name: str
    email: EmailStr
    password: str

class LoginDTO(BaseModel):
    email: EmailStr
    password: str

class ChatRequest(BaseModel):
    problemStatement: str
    code: str
    userMessage: str
    problemId: str

class UserResponse(BaseModel):
    id: int
    username: str
    email: str
    role: str

    class Config:
        from_attributes = True