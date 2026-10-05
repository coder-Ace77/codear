import hashlib
import secrets
from datetime import datetime

from fastapi import HTTPException
from sqlalchemy.orm import Session

from app.models.user import ApiKey

KEY_PREFIX = "cdr_"
MAX_ACTIVE_KEYS = 5


def hash_key(key: str) -> str:
    # Keys are 256 random bits, so a fast hash is enough: there is nothing to brute-force.
    return hashlib.sha256(key.encode("utf-8")).hexdigest()


class ApiKeyService:
    def __init__(self, db: Session):
        self.db = db

    def create(self, user_id: int, name: str):
        """Returns (row, plaintext key). The plaintext is never stored and cannot be shown again."""
        active = self.db.query(ApiKey).filter(ApiKey.user_id == user_id, ApiKey.revoked_at.is_(None)).count()
        if active >= MAX_ACTIVE_KEYS:
            raise HTTPException(
                status_code=400,
                detail=f"You can have at most {MAX_ACTIVE_KEYS} active API keys. Revoke one first.",
            )

        key = KEY_PREFIX + secrets.token_urlsafe(32)
        row = ApiKey(user_id=user_id, name=name.strip(), prefix=key[:8], key_hash=hash_key(key))
        self.db.add(row)
        self.db.commit()
        self.db.refresh(row)
        return row, key

    def list(self, user_id: int):
        return (
            self.db.query(ApiKey)
            .filter(ApiKey.user_id == user_id, ApiKey.revoked_at.is_(None))
            .order_by(ApiKey.created_at.desc())
            .all()
        )

    def revoke(self, user_id: int, key_id: int):
        # Scoped to the owner: someone else's key looks the same as one that does not exist.
        row = (
            self.db.query(ApiKey)
            .filter(ApiKey.id == key_id, ApiKey.user_id == user_id, ApiKey.revoked_at.is_(None))
            .first()
        )
        if not row:
            raise HTTPException(status_code=404, detail="API key not found")
        row.revoked_at = datetime.utcnow()
        self.db.commit()
