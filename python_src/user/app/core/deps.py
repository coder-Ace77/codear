from typing import Optional

from fastapi import Depends, Header, HTTPException
from sqlalchemy.orm import Session

from app.core import security
from app.database import get_db
from app.models.user import User
from app.services.user_service import UserService


def _bearer_token(authorization: Optional[str]) -> str:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")
    return authorization.split(" ", 1)[1].strip()


def _user_id_from(authorization: Optional[str]) -> int:
    sub = security.extract_user_id(_bearer_token(authorization))
    try:
        return int(sub)
    except (TypeError, ValueError):
        raise HTTPException(status_code=401, detail="Invalid or expired token")


def get_current_user(
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db),
) -> User:
    """The signed-in user. 401 for a missing, invalid or expired token, or a user that no longer exists."""
    user = UserService(db).get_user_by_id(_user_id_from(authorization))
    if not user:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    return user


def require_admin(
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db),
) -> User:
    """Admin-only routes. The role is read straight from the database, bypassing the cache, so a
    demoted or deleted admin loses access immediately."""
    user = UserService(db).get_user_by_id(_user_id_from(authorization), use_cache=False)
    if not user:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    if user.role != security.ROLE_ADMIN:
        raise HTTPException(status_code=403, detail="Admin access required")
    return user
