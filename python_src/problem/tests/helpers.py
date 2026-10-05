import os

from jose import jwt

# Fixed ids for the users seeded by conftest.
USER_ID = 1
OTHER_USER_ID = 2
ADMIN_ID = 3


def make_token(user_id: int = USER_ID, username: str = "tester") -> str:
    return jwt.encode(
        {"sub": str(user_id), "username": username},
        os.environ["JWT_SECRET"],
        algorithm="HS256",
    )


def bearer(user_id: int = USER_ID, username: str = "tester") -> dict:
    return {"Authorization": f"Bearer {make_token(user_id, username)}"}


def admin_bearer() -> dict:
    return bearer(ADMIN_ID, "root_admin")


def other_bearer() -> dict:
    return bearer(OTHER_USER_ID, "someone_else")
