import re
from datetime import datetime
from typing import Optional

from pydantic import BaseModel, EmailStr, Field, field_validator

from app.core.security import MAX_PASSWORD_BYTES, ROLES

USERNAME_RE = re.compile(r"^[A-Za-z0-9_.-]{3,32}$")
# Names that could be mistaken for staff. Checked case-insensitively.
RESERVED_USERNAMES = {"admin", "administrator", "root", "system", "support", "moderator", "codear"}


def _check_password(value: str) -> str:
    if len(value) < 8:
        raise ValueError("Password must be at least 8 characters")
    if len(value.encode("utf-8")) > MAX_PASSWORD_BYTES:
        raise ValueError(f"Password must be at most {MAX_PASSWORD_BYTES} bytes")
    return value


class RegisterDTO(BaseModel):
    username: str
    name: str = Field(min_length=1, max_length=100)
    email: EmailStr
    password: str

    @field_validator("username")
    @classmethod
    def valid_username(cls, v: str) -> str:
        v = v.strip()
        if not USERNAME_RE.match(v):
            raise ValueError("Username must be 3-32 characters: letters, numbers, dot, dash or underscore")
        if v.lower() in RESERVED_USERNAMES:
            raise ValueError("That username is reserved")
        return v

    @field_validator("email")
    @classmethod
    def lower_email(cls, v: str) -> str:
        return v.strip().lower()

    @field_validator("password")
    @classmethod
    def valid_password(cls, v: str) -> str:
        return _check_password(v)


class LoginDTO(BaseModel):
    email: EmailStr
    password: str = Field(max_length=256)

    @field_validator("email")
    @classmethod
    def lower_email(cls, v: str) -> str:
        return v.strip().lower()


class ChangePasswordDTO(BaseModel):
    currentPassword: str = Field(max_length=256)
    newPassword: str

    @field_validator("newPassword")
    @classmethod
    def valid_password(cls, v: str) -> str:
        return _check_password(v)


class RoleChangeDTO(BaseModel):
    role: str

    @field_validator("role")
    @classmethod
    def valid_role(cls, v: str) -> str:
        v = v.strip().upper()
        if v not in ROLES:
            raise ValueError(f"Role must be one of {', '.join(ROLES)}")
        return v


class ApiKeyCreateDTO(BaseModel):
    name: str = Field(min_length=1, max_length=60)


class ApiKeyResponse(BaseModel):
    id: int
    name: str
    prefix: str
    createdAt: datetime
    lastUsedAt: Optional[datetime] = None

    @classmethod
    def from_row(cls, row) -> "ApiKeyResponse":
        return cls(id=row.id, name=row.name, prefix=row.prefix, createdAt=row.created_at, lastUsedAt=row.last_used_at)


class ApiKeyCreated(ApiKeyResponse):
    key: str  # the only time the full key is ever returned


class ChatRequest(BaseModel):
    problemStatement: str = Field(max_length=20000)
    code: str = Field(max_length=65536)
    userMessage: str = Field(max_length=4000)
    problemId: str = Field(max_length=32)


class UserResponse(BaseModel):
    """What the API says about a user. Never includes the password hash."""

    id: int
    username: str
    name: Optional[str] = None
    email: str
    role: str
    dailyStreak: int = 0
    problemSolvedEasy: int = 0
    problemSolvedMedium: int = 0
    problemSolvedHard: int = 0
    problemSolvedTotal: int = 0

    @classmethod
    def from_user(cls, user) -> "UserResponse":
        return cls(
            id=user.id,
            username=user.username,
            name=user.name,
            email=user.email,
            role=user.role or "USER",
            dailyStreak=user.daily_streak or 0,
            problemSolvedEasy=user.problem_solved_easy or 0,
            problemSolvedMedium=user.problem_solved_medium or 0,
            problemSolvedHard=user.problem_solved_hard or 0,
            problemSolvedTotal=user.problem_solved_total or 0,
        )
