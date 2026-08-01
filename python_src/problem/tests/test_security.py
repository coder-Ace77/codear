import os
from datetime import datetime, timedelta

from jose import jwt

from app.core import security
from tests.helpers import make_token


def test_password_hash_roundtrip():
    hashed = security.get_password_hash("s3cret-password")

    assert security.verify_password("s3cret-password", hashed)
    assert not security.verify_password("wrong-password", hashed)


def test_extract_user_id_returns_an_int():
    user_id = security.extract_user_id(make_token(user_id=77))

    assert user_id == 77


def test_extract_user_id_tolerates_padding():
    assert security.extract_user_id(f"  {make_token(user_id=77)}  ") == 77


def test_extract_user_id_returns_none_for_invalid_token():
    assert security.extract_user_id("not-a-jwt") is None


def test_extract_user_id_returns_none_when_signed_with_another_key():
    token = jwt.encode({"sub": "77"}, "some-other-secret", algorithm="HS256")

    assert security.extract_user_id(token) is None


def test_extract_user_context_returns_id_and_username():
    context = security.extract_user_context(make_token(user_id=77, username="alice"))

    assert context == {"id": 77, "username": "alice"}


def test_extract_user_context_returns_none_without_subject():
    token = jwt.encode({"username": "alice"}, os.environ["JWT_SECRET"], algorithm="HS256")

    assert security.extract_user_context(token) is None


def test_is_token_expired_for_a_live_token():
    exp = datetime.utcnow() + timedelta(minutes=5)
    token = jwt.encode(
        {"sub": "77", "exp": exp}, os.environ["JWT_SECRET"], algorithm="HS256"
    )

    assert security.is_token_expired(token) is False


def test_is_token_expired_for_an_expired_token():
    exp = datetime.utcnow() - timedelta(minutes=5)
    token = jwt.encode(
        {"sub": "77", "exp": exp}, os.environ["JWT_SECRET"], algorithm="HS256"
    )

    assert security.is_token_expired(token) is True
