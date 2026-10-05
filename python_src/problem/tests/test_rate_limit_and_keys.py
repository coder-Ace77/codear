import datetime
from unittest.mock import patch

import pytest

from app.core import rate_limit
from app.core.api_keys import hash_key
from app.database import SessionLocal
from app.models.problem import ApiKey, Submission, SubmissionStatus
from tests.helpers import ADMIN_ID, OTHER_USER_ID, USER_ID, admin_bearer, bearer, other_bearer

BASE = "/api/v1/problem"


@pytest.fixture(autouse=True)
def fake_queue():
    """Submitting talks to SQS, which these tests do not need."""
    with patch("app.services.submission_service.sqs.send_to_queue") as q:
        yield q


def submit(client, problem_id, headers):
    return client.post(
        f"{BASE}/submit", headers=headers, json={"problemId": problem_id, "code": "print(1)", "language": "python"}
    )


def make_key(user_id, key="cdr_testkey_" + "a" * 20, revoked=False):
    session = SessionLocal()
    try:
        session.add(
            ApiKey(
                user_id=user_id,
                name="test",
                prefix=key[:8],
                key_hash=hash_key(key),
                revoked_at=datetime.datetime.utcnow() if revoked else None,
            )
        )
        session.commit()
    finally:
        session.close()
    return key


# --- per-user submit limit: one every 10 seconds ---


def test_a_user_can_submit_once_then_must_wait(client, created_problem):
    problem_id, _ = created_problem

    assert submit(client, problem_id, bearer()).status_code == 200
    second = submit(client, problem_id, bearer())

    assert second.status_code == 429
    assert 1 <= int(second.headers["Retry-After"]) <= 10
    assert "once every 10 seconds" in second.json()["detail"]


def test_the_limit_is_per_user(client, created_problem):
    problem_id, _ = created_problem

    assert submit(client, problem_id, bearer()).status_code == 200
    assert submit(client, problem_id, other_bearer()).status_code == 200


def test_the_user_limit_expires_after_ten_seconds(client, created_problem):
    from app.database import redis_client

    problem_id, _ = created_problem
    submit(client, problem_id, bearer())

    assert 0 < redis_client.ttl(f"rl:submit:{USER_ID}") <= 10

    redis_client.delete(f"rl:submit:{USER_ID}")  # what the passing of 10 seconds does
    assert submit(client, problem_id, bearer()).status_code == 200


def test_test_runs_are_not_rate_limited(client):
    with patch("app.api.problem_router.send_to_queue"):
        for _ in range(3):
            assert client.post(
                f"{BASE}/test",
                headers=bearer(),
                json={"code": "x", "language": "python", "problemId": 1, "input": ""},
            ).status_code == 200


# --- admins: 100 submissions per second ---


def test_an_admin_may_submit_100_per_second_but_not_101(client, created_problem):
    problem_id, _ = created_problem

    with patch.object(rate_limit, "_now", return_value=1_700_000_000.0):
        statuses = [submit(client, problem_id, admin_bearer()).status_code for _ in range(101)]

    assert statuses[:100] == [200] * 100
    assert statuses[100] == 429


def test_the_admin_budget_resets_each_second(client, created_problem):
    problem_id, _ = created_problem

    with patch.object(rate_limit, "_now", return_value=1_700_000_000.0):
        for _ in range(100):
            submit(client, problem_id, admin_bearer())
        assert submit(client, problem_id, admin_bearer()).status_code == 429

    with patch.object(rate_limit, "_now", return_value=1_700_000_001.0):
        assert submit(client, problem_id, admin_bearer()).status_code == 200


def test_the_limiter_fails_open_when_redis_is_down(client, created_problem):
    problem_id, _ = created_problem

    with patch.object(rate_limit.redis_client, "set", side_effect=ConnectionError("down")):
        assert submit(client, problem_id, bearer()).status_code == 200


# --- API keys ---


def test_an_api_key_can_submit_and_read_the_result(client, created_problem):
    problem_id, _ = created_problem
    key = make_key(USER_ID)

    sent = submit(client, problem_id, {"X-API-Key": key})
    assert sent.status_code == 200
    sub_id = sent.json()["submissionId"]

    with patch("app.services.submission_service.cache.get_cache", return_value="PASSED"):
        result = client.get(f"{BASE}/submissions/{sub_id}", headers={"X-API-Key": key})

    assert result.status_code == 200
    assert result.json()["submissionId"] == sub_id


def test_a_key_also_works_as_a_bearer_token(client, created_problem):
    problem_id, _ = created_problem
    key = make_key(USER_ID)

    assert submit(client, problem_id, {"Authorization": f"Bearer {key}"}).status_code == 200


def test_a_key_is_the_same_user_as_the_session_for_rate_limiting(client, created_problem):
    problem_id, _ = created_problem
    key = make_key(USER_ID)

    assert submit(client, problem_id, bearer()).status_code == 200
    # one budget per user, however they authenticate
    assert submit(client, problem_id, {"X-API-Key": key}).status_code == 429


def test_a_key_only_sees_its_owners_submissions(client, created_problem):
    problem_id, _ = created_problem
    key = make_key(USER_ID)
    session = SessionLocal()
    try:
        session.add(
            Submission(
                submission_id="theirs", user_id=OTHER_USER_ID, problem_id=problem_id,
                code="x", language="python", status=SubmissionStatus.PASSED,
            )
        )
        session.commit()
    finally:
        session.close()

    with patch("app.services.submission_service.cache.get_cache", return_value="PASSED"):
        assert client.get(f"{BASE}/submissions/theirs", headers={"X-API-Key": key}).status_code == 404


def test_a_revoked_key_stops_working(client, created_problem):
    problem_id, _ = created_problem
    key = make_key(USER_ID, revoked=True)

    assert submit(client, problem_id, {"X-API-Key": key}).status_code == 401


def test_an_unknown_key_is_rejected(client, created_problem):
    problem_id, _ = created_problem

    assert submit(client, problem_id, {"X-API-Key": "cdr_not-a-real-key"}).status_code == 401


def test_the_key_records_when_it_was_last_used(client, created_problem):
    problem_id, _ = created_problem
    key = make_key(USER_ID)

    submit(client, problem_id, {"X-API-Key": key})

    session = SessionLocal()
    try:
        assert session.query(ApiKey).one().last_used_at is not None
    finally:
        session.close()


def test_an_admins_api_key_cannot_do_admin_things(client, problem_payload):
    key = make_key(ADMIN_ID)

    assert client.post(f"{BASE}/addproblem", json=problem_payload(), headers={"X-API-Key": key}).status_code == 403
    assert client.get(f"{BASE}/admin/stats", headers={"X-API-Key": key}).status_code == 403


def test_a_key_for_a_deleted_user_is_rejected(client, created_problem):
    problem_id, _ = created_problem
    key = make_key(9999)  # no such user

    assert submit(client, problem_id, {"X-API-Key": key}).status_code == 401
