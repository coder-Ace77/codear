import os
from datetime import datetime, timedelta
from typing import Optional
from jose import JWTError, jwt
import bcrypt
from dotenv import load_dotenv

load_dotenv()

# Must match the Secret Key in the User Service
SECRET_KEY = os.getenv("JWT_SECRET")
ALGORITHM = "HS256"

def get_password_hash(password: str) -> str:
    """Hashes a password using bcrypt."""
    pwd_bytes = password.encode('utf-8')
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(pwd_bytes, salt)
    return hashed.decode('utf-8')

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verifies a plain password against a hash."""
    return bcrypt.checkpw(
        plain_password.encode('utf-8'), 
        hashed_password.encode('utf-8')
    )

def extract_user_id(token: str) -> Optional[int]:
    """
    Equivalent to jwtService.extractUserId(token) in Java.
    Decodes the token and returns the 'sub' (subject) which is the user ID.
    """
    try:
        # Strip whitespace just like token.strip() in your Java code
        payload = jwt.decode(token.strip(), SECRET_KEY, algorithms=[ALGORITHM])
        user_id_str: str = payload.get("sub")
        if user_id_str is None:
            return None
        return int(user_id_str)
    except JWTError as e:
        print(f"JWT Decode Error: {str(e)}")
        return None
    except ValueError:
        return None

def extract_user_context(token: str) -> dict:
    """
    Decodes the token and returns 'id' and 'username'.
    """
    try:
        payload = jwt.decode(token.strip(), SECRET_KEY, algorithms=[ALGORITHM])
        user_id_str = payload.get("sub")
        username = payload.get("username")
        
        if not user_id_str:
            return None
            
        return {
            "id": int(user_id_str),
            "username": username
        }
    except JWTError:
        return None
    except ValueError:
        return None

def is_token_expired(token: str) -> bool:
    """Checks if the token is expired."""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        exp = payload.get("exp")
        return datetime.utcfromtimestamp(exp) < datetime.utcnow()
    except JWTError:
        return True