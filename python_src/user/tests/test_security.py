from app.core import security


def test_password_hash_roundtrip():
    hashed = security.get_password_hash("s3cret-password")

    assert hashed != "s3cret-password"
    assert security.verify_password("s3cret-password", hashed)


def test_verify_password_rejects_wrong_password():
    hashed = security.get_password_hash("s3cret-password")

    assert not security.verify_password("wrong-password", hashed)


def test_access_token_carries_user_id():
    token = security.create_access_token({"sub": "42", "username": "someone"})

    assert security.extract_user_id(token) == "42"


def test_extract_user_id_returns_none_for_invalid_token():
    assert security.extract_user_id("not-a-jwt") is None


def test_extract_user_id_returns_none_for_foreign_signature():
    token = security.create_access_token({"sub": "42"})
    tampered = token[:-4] + ("aaaa" if not token.endswith("aaaa") else "bbbb")

    assert security.extract_user_id(tampered) is None
