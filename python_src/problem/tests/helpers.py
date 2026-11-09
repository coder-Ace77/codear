import os

from jose import jwt

USER_ID = 1


def make_token(user_id: int = USER_ID, username: str = "tester") -> str:
    return jwt.encode(
        {"sub": str(user_id), "username": username},
        os.environ["JWT_SECRET"],
        algorithm="HS256",
    )
