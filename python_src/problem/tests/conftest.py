import os

# Defaults for testing env
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
os.environ.setdefault("SQS_ACCESS_KEY", "test")
os.environ.setdefault("SQS_SECRET_KEY", "test")

import pytest
from fastapi.testclient import TestClient

from app.core.local_cache import LocalCache
from app.database import SessionLocal, redis_client
from app.main import app
from app.models.problem import Editorial, Problem, Submission, TestCase, User
from tests.helpers import ADMIN_ID, OTHER_USER_ID, USER_ID, admin_bearer, bearer


@pytest.fixture(scope="session")
def client():
    with TestClient(app) as c:
        yield c


@pytest.fixture
def auth_header():
    return bearer()


@pytest.fixture
def admin_header():
    return admin_bearer()


@pytest.fixture(autouse=True)
def clean_state():
    """Postgres, Redis and the in-process LocalCache all outlive a single test."""
    session = SessionLocal()
    try:
        session.query(Editorial).delete()
        session.query(Submission).delete()
        session.query(TestCase).delete()
        session.query(Problem).delete()
        session.query(User).delete()
        session.add_all(
            [
                User(id=USER_ID, username="tester", role="USER"),
                User(id=OTHER_USER_ID, username="someone_else", role="USER"),
                User(id=ADMIN_ID, username="root_admin", role="ADMIN"),
            ]
        )
        session.commit()
    finally:
        session.close()
    redis_client.flushdb()
    LocalCache.clear()
    yield


def _problem_payload(**overrides):
    payload = {
        "title": "Two Sum",
        "description": "Find two numbers in an array that add up to a target",
        "inputDescription": "An array and a target",
        "outputDescription": "Two indices",
        "constraints": "n <= 1000",
        "difficulty": "easy",
        "tags": ["array", "hashmap"],
        "timeLimitMs": 1000,
        "memoryLimitMb": 256,
        "testCases": [
            {"input": "2 7 11 15\n9", "output": "0 1", "isSample": True},
            {"input": "3 2 4\n6", "output": "1 2", "isSample": False},
        ],
    }
    payload.update(overrides)
    return payload


@pytest.fixture
def problem_payload():
    """Factory for a valid /addproblem body."""
    return _problem_payload


@pytest.fixture
def add_problem(client, admin_header):
    """Creates a problem through the API (as an admin) and returns its id."""

    def _add(**overrides):
        response = client.post(
            "/api/v1/problem/addproblem",
            json=_problem_payload(**overrides),
            headers=admin_header,
        )
        assert response.status_code == 200, response.text
        return response.json()["id"]

    return _add


@pytest.fixture
def created_problem(add_problem):
    """A problem in the DB; returns (id, payload)."""
    payload = _problem_payload()
    return add_problem(), payload


@pytest.fixture
def db_user_named_admin():
    """A plain USER whose username is "admin": the name must not grant anything."""
    session = SessionLocal()
    try:
        session.add(User(id=50, username="admin", role="USER"))
        session.commit()
    finally:
        session.close()
    return 50
