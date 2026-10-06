from unittest.mock import patch

import pytest

from app.core import security
from app.database import SessionLocal
from app.models.problem import Submission, SubmissionStatus, User
from tests.helpers import ADMIN_ID, OTHER_USER_ID, USER_ID, admin_bearer, bearer, make_token, other_bearer

BASE = "/api/v1/problem"


def make_submission(user_id, problem_id, sub_id):
    session = SessionLocal()
    try:
        session.add(
            Submission(
                submission_id=sub_id,
                user_id=user_id,
                problem_id=problem_id,
                code="print(1)",
                language="python",
                status=SubmissionStatus.PASSED,
            )
        )
        session.commit()
    finally:
        session.close()


@pytest.fixture(autouse=True)
def no_wait():
    """Judging is not under test here, so skip the 10 second long-poll sleep."""
    with patch("app.services.submission_service.cache.get_cache", return_value="PASSED"):
        yield


# --- who may create and delete problems ---


def test_adding_a_problem_requires_a_token(client, problem_payload):
    assert client.post(f"{BASE}/addproblem", json=problem_payload()).status_code == 401


def test_a_regular_user_cannot_add_a_problem(client, problem_payload, auth_header):
    response = client.post(f"{BASE}/addproblem", json=problem_payload(), headers=auth_header)

    assert response.status_code == 403


def test_an_admin_can_add_a_problem(client, problem_payload, admin_header):
    response = client.post(f"{BASE}/addproblem", json=problem_payload(), headers=admin_header)

    assert response.status_code == 200


def test_deleting_a_problem_requires_an_admin(client, created_problem, auth_header, admin_header):
    problem_id, _ = created_problem

    assert client.delete(f"{BASE}/{problem_id}").status_code == 401
    assert client.delete(f"{BASE}/{problem_id}", headers=auth_header).status_code == 403
    assert client.delete(f"{BASE}/{problem_id}", headers=admin_header).status_code == 200


def test_a_token_claiming_admin_is_not_trusted(client, problem_payload):
    forged = security.jwt.encode(
        {"sub": str(USER_ID), "username": "tester", "role": "ADMIN"},
        security.SECRET_KEY,
        algorithm="HS256",
    )

    response = client.post(
        f"{BASE}/addproblem", json=problem_payload(), headers={"Authorization": f"Bearer {forged}"}
    )

    assert response.status_code == 403


def test_a_deleted_admin_loses_access_immediately(client, problem_payload, admin_header):
    session = SessionLocal()
    try:
        session.query(User).filter(User.id == ADMIN_ID).delete()
        session.commit()
    finally:
        session.close()

    response = client.post(f"{BASE}/addproblem", json=problem_payload(), headers=admin_header)

    assert response.status_code == 401


def test_a_demoted_admin_loses_access_immediately(client, problem_payload, admin_header):
    session = SessionLocal()
    try:
        session.query(User).filter(User.id == ADMIN_ID).update({"role": "USER"})
        session.commit()
    finally:
        session.close()

    response = client.post(f"{BASE}/addproblem", json=problem_payload(), headers=admin_header)

    assert response.status_code == 403


def test_token_signed_with_another_key_is_rejected(client, problem_payload):
    bad = security.jwt.encode({"sub": str(ADMIN_ID)}, "some-other-secret", algorithm="HS256")

    response = client.post(
        f"{BASE}/addproblem", json=problem_payload(), headers={"Authorization": f"Bearer {bad}"}
    )

    assert response.status_code == 401


# --- reading submissions ---


def test_a_submission_is_readable_by_its_owner(client, created_problem):
    problem_id, _ = created_problem
    make_submission(USER_ID, problem_id, "sub-1")

    response = client.get(f"{BASE}/submissions/sub-1", headers=bearer())

    assert response.status_code == 200
    assert response.json()["submissionId"] == "sub-1"


def test_a_submission_is_hidden_from_other_users(client, created_problem):
    problem_id, _ = created_problem
    make_submission(USER_ID, problem_id, "sub-2")

    response = client.get(f"{BASE}/submissions/sub-2", headers=other_bearer())

    assert response.status_code == 404


def test_a_submission_is_not_readable_anonymously(client, created_problem):
    problem_id, _ = created_problem
    make_submission(USER_ID, problem_id, "sub-3")

    assert client.get(f"{BASE}/submissions/sub-3").status_code == 401


def test_an_admin_can_read_any_submission(client, created_problem):
    problem_id, _ = created_problem
    make_submission(USER_ID, problem_id, "sub-4")

    assert client.get(f"{BASE}/submissions/sub-4", headers=admin_bearer()).status_code == 200


def test_a_missing_submission_looks_the_same_as_someone_elses(client):
    assert client.get(f"{BASE}/submissions/nope", headers=bearer()).status_code == 404


def test_submission_list_only_has_the_callers_own(client, created_problem):
    problem_id, _ = created_problem
    make_submission(USER_ID, problem_id, "mine")
    make_submission(OTHER_USER_ID, problem_id, "theirs")

    response = client.get(f"{BASE}/submissions/subuser/{problem_id}", headers=bearer())

    assert [s["submissionId"] for s in response.json()] == ["mine"]


def test_submission_list_requires_a_token(client, created_problem):
    problem_id, _ = created_problem

    assert client.get(f"{BASE}/submissions/subuser/{problem_id}").status_code == 401


# --- running code ---


def test_submitting_requires_a_token(client, created_problem):
    problem_id, _ = created_problem

    response = client.post(
        f"{BASE}/submit", json={"problemId": problem_id, "code": "x", "language": "python"}
    )

    assert response.status_code == 401


def test_test_runs_require_a_token(client):
    response = client.post(
        f"{BASE}/test", json={"code": "x", "language": "python", "problemId": 1, "input": ""}
    )

    assert response.status_code == 401


def test_a_test_run_belongs_to_the_caller_not_the_body(client):
    with patch("app.api.problem_router.send_to_queue") as queue:
        response = client.post(
            f"{BASE}/test",
            headers=bearer(),
            json={"userId": OTHER_USER_ID, "code": "x", "language": "python", "problemId": 1, "input": ""},
        )

    assert response.status_code == 200
    assert queue.call_args.args[0]["userId"] == USER_ID


def test_test_run_results_are_private(client):
    with patch("app.api.problem_router.send_to_queue"):
        run = client.post(
            f"{BASE}/test",
            headers=bearer(),
            json={"code": "x", "language": "python", "problemId": 1, "input": ""},
        ).json()["submissionId"]

    assert client.get(f"{BASE}/submissions/test/{run}", headers=bearer()).status_code == 200
    assert client.get(f"{BASE}/submissions/test/{run}", headers=other_bearer()).status_code == 404
    assert client.get(f"{BASE}/submissions/test/{run}").status_code == 401


def test_oversized_code_is_rejected(client, created_problem):
    problem_id, _ = created_problem

    response = client.post(
        f"{BASE}/submit",
        headers=bearer(),
        json={"problemId": problem_id, "code": "x" * 70000, "language": "python"},
    )

    assert response.status_code == 422


# --- errors do not leak internals ---


def test_unexpected_errors_are_generic(created_problem):
    from fastapi.testclient import TestClient

    from app.main import app

    problem_id, _ = created_problem

    # the shared client re-raises server errors; this one returns the response a caller would see
    with TestClient(app, raise_server_exceptions=False) as quiet:
        with patch("app.api.problem_router.ProblemService.get_problem_by_id", side_effect=RuntimeError("secret db host")):
            response = quiet.get(f"{BASE}/problem/{problem_id}")

    assert response.status_code == 500
    assert "secret" not in response.text


def test_hidden_test_cases_are_not_exposed(client, created_problem):
    problem_id, _ = created_problem

    cases = client.get(f"{BASE}/problem/{problem_id}").json()["testCases"]

    assert all(c["isSample"] for c in cases)


# --- long polling must not hold a database connection while it waits ---


def test_long_poll_releases_its_database_connection_while_waiting(created_problem):
    import asyncio

    from app.core.auth import CurrentUser
    from app.services.submission_service import SubmissionService

    problem_id, _ = created_problem
    make_submission(USER_ID, problem_id, "slow-one")
    session = SessionLocal()
    in_transaction_while_waiting = []
    answers = iter(["IN_PROGRESS", "IN_PROGRESS", "PASSED"])

    async def fake_sleep(_seconds):
        in_transaction_while_waiting.append(session.in_transaction())

    try:
        with patch("app.services.submission_service.cache.get_cache", side_effect=lambda _k: next(answers)), patch(
            "app.services.submission_service.asyncio.sleep", fake_sleep
        ):
            result = asyncio.run(
                SubmissionService(session).long_poll_submission("slow-one", CurrentUser(USER_ID, "tester", "USER"))
            )
    finally:
        session.close()

    assert result.submission_id == "slow-one"
    assert in_transaction_while_waiting == [False, False]
