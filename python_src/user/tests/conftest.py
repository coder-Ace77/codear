import os
import uuid

# Defaules for testing env
os.environ.setdefault("DB_USER", "postgres")
os.environ.setdefault("PASSWORD", "postgres")
os.environ.setdefault("DB_HOST", "localhost")
os.environ.setdefault("DB_PORT", "5432")
os.environ.setdefault("DB_NAME", "postgres")
os.environ.setdefault("DB_SSL_MODE", "disable")
os.environ.setdefault("REDIS_HOST", "localhost")
os.environ.setdefault("REDIS_PORT", "6379")
os.environ.setdefault("REDIS_SSL", "false")
os.environ.setdefault("JWT_SECRET", "test-secret")
os.environ.setdefault("JWT_EXPIRY", "60")

import pytest
from fastapi.testclient import TestClient

from app.database import SessionLocal, redis_client
from app.main import app
from app.models.user import ChatMessage, User


@pytest.fixture(scope="session")
def client():
    with TestClient(app) as c:
        yield c


@pytest.fixture
def db():
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture(autouse=True)
def clean_state():
    """Postgres and Redis are shared across tests, so reset both between them."""
    session = SessionLocal()
    try:
        session.query(ChatMessage).delete()
        session.query(User).delete()
        session.commit()
    finally:
        session.close()
    redis_client.flushdb()
    yield


@pytest.fixture
def new_user():
    """A registration payload with unique username/email."""
    suffix = uuid.uuid4().hex[:8]
    return {
        "username": f"user_{suffix}",
        "name": "Test User",
        "email": f"user_{suffix}@example.com",
        "password": "s3cret-password",
    }
