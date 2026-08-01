import os

from jose import jwt


def make_token(user_id: int = 1, username: str = "tester") -> str:
    return jwt.encode(
        {"sub": str(user_id), "username": username},
        os.environ["JWT_SECRET"],
        algorithm="HS256",
    )


def bearer(user_id: int = 1, username: str = "tester") -> dict:
    return {"Authorization": f"Bearer {make_token(user_id, username)}"}
