from dataclasses import dataclass
from typing import Optional

from fastapi import Depends, Header, HTTPException
from sqlalchemy.orm import Session

from app.core import security
from app.database import get_db
from app.models.problem import User

ROLE_ADMIN = "ADMIN"


@dataclass(frozen=True)
class CurrentUser:
    id: int
    username: str
    role: str

    @property
    def is_admin(self) -> bool:
        return self.role == ROLE_ADMIN


def _load(authorization: Optional[str], db: Session) -> CurrentUser:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

    context = security.extract_user_context(authorization.split(" ", 1)[1])
    if not context:
        raise HTTPException(status_code=401, detail="Invalid or expired token")

    # The role comes from the database on every request, never from the token, so promoting,
    # demoting or deleting a user takes effect immediately.
    row = db.query(User).filter(User.id == context["id"]).first()
    if not row:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    return CurrentUser(id=row.id, username=row.username, role=row.role or "USER")


def get_current_user(
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db),
) -> CurrentUser:
    return _load(authorization, db)


def get_optional_user(
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db),
) -> Optional[CurrentUser]:
    """For public routes that behave differently for admins. A bad token is treated as anonymous."""
    if not authorization:
        return None
    try:
        return _load(authorization, db)
    except HTTPException:
        return None


def require_admin(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    if not user.is_admin:
        raise HTTPException(status_code=403, detail="Admin access required")
    return user
