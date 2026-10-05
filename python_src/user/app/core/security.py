import os
from datetime import datetime, timedelta
from typing import Optional

from dotenv import load_dotenv
from jose import JWTError, jwt
from passlib.context import CryptContext

load_dotenv()

SECRET_KEY = os.getenv("JWT_SECRET")
if not SECRET_KEY:
    # Without a secret every token would be signed with "None": refuse to start instead.
    raise RuntimeError("JWT_SECRET is not set")

ALGORITHM = "HS256"
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Roles. Authorisation always reads the role from the database, never from a token.
ROLE_USER = "USER"
ROLE_ADMIN = "ADMIN"
ROLES = (ROLE_USER, ROLE_ADMIN)

# JWT_EXPIRY is in minutes. Older deployments set it in milliseconds (86400000), which
# would mint tokens valid for ~164 years, so tokens never live longer than a day.
MAX_TOKEN_MINUTES = 24 * 60

# bcrypt only looks at the first 72 bytes of a password.
MAX_PASSWORD_BYTES = 72

# A hash to verify against when an email is unknown, so unknown and known emails take the same time.
_DUMMY_HASH = pwd_context.hash("not-a-real-password")


def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)


def verify_password_or_dummy(plain_password: str, hashed_password: Optional[str]) -> bool:
    """Runs a bcrypt verification even when there is no hash, to blunt account enumeration by timing."""
    if hashed_password is None:
        pwd_context.verify(plain_password, _DUMMY_HASH)
        return False
    return pwd_context.verify(plain_password, hashed_password)


def get_password_hash(password):
    return pwd_context.hash(password)


def token_minutes() -> int:
    try:
        minutes = int(os.getenv("JWT_EXPIRY", 60))
    except ValueError:
        minutes = 60
    return max(1, min(minutes, MAX_TOKEN_MINUTES))


def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=token_minutes())
    to_encode.update({"exp": expire, "sub": str(data.get("sub"))})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


def extract_user_id(token: str):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload.get("sub")
    except JWTError:
        return None
