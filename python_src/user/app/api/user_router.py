from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.core.deps import get_current_user, require_admin
from app.database import get_db
from app.models.user import User
from app.schemas.user_schema import (
    ApiKeyCreated,
    ApiKeyCreateDTO,
    ApiKeyResponse,
    ChangePasswordDTO,
    ChatRequest,
    LoginDTO,
    RegisterDTO,
    RoleChangeDTO,
    UserResponse,
)
from app.services.ai_service import AiService
from app.services.api_key_service import ApiKeyService
from app.services.user_service import UserService

router = APIRouter(prefix="/api/v1/user")


@router.post("/register", response_model=UserResponse)
def register(data: RegisterDTO, db: Session = Depends(get_db)):
    return UserResponse.from_user(UserService(db).register_user(data))


@router.post("/login")
def login(data: LoginDTO, db: Session = Depends(get_db)):
    token = UserService(db).login_user(data)
    if not token:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return {"token": token}


@router.get("/user", response_model=UserResponse)
def get_user(user: User = Depends(get_current_user)):
    return UserResponse.from_user(user)


@router.post("/change-password")
def change_password(
    data: ChangePasswordDTO,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    UserService(db).change_password(user, data)
    return {"message": "Password updated"}


# --- API keys. Managing keys needs a signed-in session (a JWT), so a leaked key cannot mint more. ---


@router.post("/api-keys", response_model=ApiKeyCreated)
def create_api_key(
    body: ApiKeyCreateDTO,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    row, key = ApiKeyService(db).create(user.id, body.name)
    return ApiKeyCreated(**ApiKeyResponse.from_row(row).model_dump(), key=key)


@router.get("/api-keys", response_model=List[ApiKeyResponse])
def list_api_keys(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return [ApiKeyResponse.from_row(r) for r in ApiKeyService(db).list(user.id)]


@router.delete("/api-keys/{key_id}")
def revoke_api_key(key_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    ApiKeyService(db).revoke(user.id, key_id)
    return {"message": "API key revoked"}


@router.post("/chat")
async def chat(
    request: ChatRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # The rate limiter updates the user row, so it needs a database-attached copy, not the cached one.
    db_user = UserService(db).get_user_by_id(user.id, use_cache=False)
    reply = await AiService(db).get_ai_response(
        db_user,
        request.problemStatement,
        request.code,
        request.userMessage,
        request.problemId,
    )
    return {"reply": reply}


@router.get("/chat/history/{problemId}")
async def get_chat_history(
    problemId: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return await AiService(db).get_chat_history(user.id, problemId)


# --- Admin only ---


@router.get("/admin/users", response_model=List[UserResponse])
def list_users(_admin: User = Depends(require_admin), db: Session = Depends(get_db)):
    return [UserResponse.from_user(u) for u in UserService(db).list_users()]


@router.patch("/admin/users/{user_id}/role", response_model=UserResponse)
def change_role(
    user_id: int,
    body: RoleChangeDTO,
    admin: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    return UserResponse.from_user(UserService(db).set_role(user_id, body.role, admin))
