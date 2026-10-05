from datetime import datetime

from fastapi import HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core import security
from app.models.user import User
from app.schemas.user_schema import ChangePasswordDTO, LoginDTO, RegisterDTO
from app.services.login_throttle import LoginThrottle

# Cached copies never hold the password hash.
CACHED_COLUMNS_EXCLUDED = {"password"}


class UserService:
    def __init__(self, db: Session):
        self.db = db

    def register_user(self, data: RegisterDTO):
        # Email is lower-cased by the schema; usernames are compared case-insensitively so
        # "Admin" and "admin" cannot both exist.
        if self._by_email(data.email):
            raise HTTPException(status_code=400, detail="Email already in use")

        if self.db.query(User).filter(func.lower(User.username) == data.username.lower()).first():
            raise HTTPException(status_code=400, detail="Username already taken")

        new_user = User(
            username=data.username,
            name=data.name.strip(),
            email=data.email,
            password=security.get_password_hash(data.password),
            role=security.ROLE_USER,  # registration can never create an admin
            daily_streak=0,
            problem_solved_total=0,
        )

        self.db.add(new_user)
        self.db.commit()
        self.db.refresh(new_user)
        return new_user

    def login_user(self, data: LoginDTO):
        """Returns a token, None for bad credentials, and raises 429 when throttled."""
        if LoginThrottle.is_blocked(data.email):
            raise HTTPException(status_code=429, detail="Too many failed attempts. Try again in a few minutes.")

        # Credentials are always checked against the database, never the cache.
        user = self._by_email(data.email)
        valid = security.verify_password_or_dummy(data.password, user.password if user else None)
        if not user or not valid:
            LoginThrottle.record_failure(data.email)
            return None

        LoginThrottle.clear(data.email)
        return security.create_access_token(
            data={"sub": str(user.id), "username": user.username, "role": user.role}
        )

    def change_password(self, user: User, data: ChangePasswordDTO):
        # `user` may be a cached copy without a hash, so reload the real row.
        row = self.db.query(User).filter(User.id == user.id).first()
        if not row or not security.verify_password(data.currentPassword, row.password):
            raise HTTPException(status_code=400, detail="Current password is incorrect")
        row.password = security.get_password_hash(data.newPassword)
        self.db.commit()

    def set_role(self, target_id: int, role: str, acting_admin: User) -> User:
        target = self.db.query(User).filter(User.id == target_id).first()
        if not target:
            raise HTTPException(status_code=404, detail="User not found")
        if target.id == acting_admin.id and role != security.ROLE_ADMIN:
            raise HTTPException(status_code=400, detail="You cannot remove your own admin role")
        target.role = role
        self.db.commit()
        self.invalidate(target.id)
        return target

    def list_users(self):
        return self.db.query(User).order_by(User.id).all()

    def _by_email(self, email: str):
        return self.db.query(User).filter(func.lower(User.email) == email.lower()).first()

    def get_user_by_email(self, email: str):
        return self._by_email(email.strip().lower())

    @staticmethod
    def invalidate(user_id: int):
        from app.services.cache_service import CacheService

        try:
            CacheService.delete(f"user:{user_id}")
        except Exception:
            pass

    def get_user_by_id(self, user_id: int, use_cache: bool = True):
        from app.services.cache_service import CacheService

        cache_key = f"user:{user_id}"

        if use_cache:
            try:
                cached_data = CacheService.get_object(cache_key)
            except Exception:
                cached_data = None
            if cached_data:
                if cached_data.get("last_chat_reset"):
                    cached_data["last_chat_reset"] = datetime.fromisoformat(cached_data["last_chat_reset"])
                return User(**cached_data)

        user = self.db.query(User).filter(User.id == user_id).first()

        if user and use_cache:
            user_dict = {
                c.name: getattr(user, c.name)
                for c in user.__table__.columns
                if c.name not in CACHED_COLUMNS_EXCLUDED
            }
            if isinstance(user_dict.get("last_chat_reset"), datetime):
                user_dict["last_chat_reset"] = user_dict["last_chat_reset"].isoformat()
            try:
                CacheService.set_object(cache_key, user_dict)
            except Exception:
                pass  # a cache outage must not break the request

        return user
