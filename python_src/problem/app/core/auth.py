import datetime
from dataclasses import dataclass
from typing import Optional

from fastapi import Depends, Header, HTTPException
from sqlalchemy.orm import Session

from app.core import security
from app.core.api_keys import KEY_PREFIX, hash_key
from app.database import get_db
from app.models.problem import ApiKey, User

ROLE_ADMIN = "ADMIN"

# last_used_at is only rewritten when it is older than this, so a busy key is not a write per request.
LAST_USED_GRANULARITY = datetime.timedelta(minutes=1)


@dataclass(frozen=True)
class CurrentUser:
    id: int
    username: str
    role: str
    via_api_key: bool = False

    @property
    def is_admin(self) -> bool:
        return self.role == ROLE_ADMIN


def _from_user_row(row: User, via_api_key: bool) -> CurrentUser:
    return CurrentUser(id=row.id, username=row.username, role=row.role or "USER", via_api_key=via_api_key)


def _load_from_key(key: str, db: Session) -> CurrentUser:
    key_row = db.query(ApiKey).filter(ApiKey.key_hash == hash_key(key.strip())).first()
    if not key_row or key_row.revoked_at is not None:
        raise HTTPException(status_code=401, detail="Invalid or revoked API key")

    row = db.query(User).filter(User.id == key_row.user_id).first()
    if not row:
        raise HTTPException(status_code=401, detail="Invalid or revoked API key")

    now = datetime.datetime.utcnow()
    if key_row.last_used_at is None or now - key_row.last_used_at > LAST_USED_GRANULARITY:
        key_row.last_used_at = now
        db.commit()

    return _from_user_row(row, via_api_key=True)


def _load_from_token(token: str, db: Session) -> CurrentUser:
    context = security.extract_user_context(token)
    if not context:
        raise HTTPException(status_code=401, detail="Invalid or expired token")

    # The role comes from the database on every request, never from the token, so promoting,
    # demoting or deleting a user takes effect immediately.
    row = db.query(User).filter(User.id == context["id"]).first()
    if not row:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    return _from_user_row(row, via_api_key=False)


def _load(authorization: Optional[str], api_key: Optional[str], db: Session) -> CurrentUser:
    """Accepts a session token (Authorization: Bearer <jwt>) or an API key, sent either as
    `X-API-Key: cdr_...` or `Authorization: Bearer cdr_...`."""
    if api_key:
        return _load_from_key(api_key, db)

    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

    token = authorization.split(" ", 1)[1].strip()
    if token.startswith(KEY_PREFIX):
        return _load_from_key(token, db)
    return _load_from_token(token, db)


def get_current_user(
    authorization: Optional[str] = Header(None),
    x_api_key: Optional[str] = Header(None),
    db: Session = Depends(get_db),
) -> CurrentUser:
    return _load(authorization, x_api_key, db)


def get_optional_user(
    authorization: Optional[str] = Header(None),
    x_api_key: Optional[str] = Header(None),
    db: Session = Depends(get_db),
) -> Optional[CurrentUser]:
    """For public routes that behave differently for admins. A bad credential is treated as anonymous."""
    if not authorization and not x_api_key:
        return None
    try:
        return _load(authorization, x_api_key, db)
    except HTTPException:
        return None


def require_admin(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    if user.via_api_key:
        # An API key is for submitting code. Admin actions need a signed-in session.
        raise HTTPException(status_code=403, detail="Admin actions need a signed-in session, not an API key")
    if not user.is_admin:
        raise HTTPException(status_code=403, detail="Admin access required")
    return user
