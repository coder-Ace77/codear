import asyncio
import threading
import time
from unittest.mock import patch

import pytest

from app.core import progress
from app.database import SessionLocal, redis_client
from app.models.problem import Submission, SubmissionStatus
from app.services.submission_service import SubmissionService
from tests.helpers import ADMIN_ID, OTHER_USER_ID, USER_ID, admin_bearer, bearer, other_bearer

BASE = "/api/v1/problem/submissions"


def make_submission(sub_id="s1", user_id=USER_ID, status=SubmissionStatus.IN_PROGRESS, language="python", **extra):
    session = SessionLocal()
    try:
        session.add(Submission(submission_id=sub_id, user_id=user_id, problem_id=1, code="print(1)",
                               language=language, status=status, **extra))
        session.commit()
    finally:
        session.close()


def set_progress(sub_id, v, stage, total=None, started=0, completed=0):
    progress.write(sub_id, {"v": v, "stage": stage, "total": total, "started": started, "completed": completed})


def get(client, sub_id, since=0, wait=0, headers=None):
    return client.get(f"{BASE}/{sub_id}/progress?since={since}&wait={wait}", headers=headers or bearer())


# --- access ---


def test_progress_needs_a_token(client):
    make_submission()
    assert client.get(f"{BASE}/s1/progress").status_code == 401


def test_progress_is_private_to_the_owner(client):
    make_submission()
    assert get(client, "s1", headers=other_bearer()).status_code == 404
    assert get(client, "s1", headers=bearer()).status_code == 200


def test_an_admin_can_watch_any_submission(client):
    make_submission()
    assert get(client, "s1", headers=admin_bearer()).status_code == 200


def test_unknown_submission_is_a_404(client):
    assert get(client, "nope").status_code == 404


def test_wait_is_bounded(client):
    make_submission()
    assert client.get(f"{BASE}/s1/progress?wait=99", headers=bearer()).status_code == 422
    assert client.get(f"{BASE}/s1/progress?since=-1", headers=bearer()).status_code == 422


# --- the version cursor ---


def test_first_call_returns_the_queued_state_at_once(client):
    make_submission()
    set_progress("s1", 1, "QUEUED")

    body = get(client, "s1", since=0).json()

    assert (body["stage"], body["version"], body["changed"], body["terminal"]) == ("QUEUED", 1, True, False)
    assert body["percent"] == 5 and body["message"] == "Queued" and body["status"] == "IN_PROGRESS"


def test_nothing_new_returns_the_same_state_marked_unchanged(client):
    make_submission()
    set_progress("s1", 3, "RUNNING", total=10, started=2, completed=1)

    body = get(client, "s1", since=3, wait=0).json()

    assert body["changed"] is False
    assert body["version"] == 3 and body["stage"] == "RUNNING"


def test_a_newer_version_is_returned_without_waiting(client):
    make_submission()
    set_progress("s1", 4, "RUNNING", total=10, started=3, completed=2)

    started = time.monotonic()
    body = get(client, "s1", since=3, wait=20).json()

    assert body["changed"] is True and body["version"] == 4
    assert time.monotonic() - started < 3


def test_the_call_wakes_up_as_soon_as_the_engine_reports(client):
    make_submission()
    set_progress("s1", 2, "PREPARING", total=10)
    timer = threading.Timer(0.6, set_progress, args=("s1", 3, "RUNNING", 10, 1, 0))
    timer.start()

    started = time.monotonic()
    body = get(client, "s1", since=2, wait=15).json()
    waited = time.monotonic() - started
    timer.join()

    assert body["changed"] is True and body["stage"] == "RUNNING" and body["version"] == 3
    assert 0.4 < waited < 4, waited


# --- what the numbers and words say ---


@pytest.mark.parametrize(
    "stage,total,started,completed,language,percent,message",
    [
        ("QUEUED", None, 0, 0, "python", 5, "Queued"),
        ("PREPARING", 26, 0, 0, "python", 15, "Preparing the sandbox"),
        ("RUNNING", 26, 0, 0, "cpp", 30, "Compiling"),
        ("RUNNING", 26, 0, 0, "python", 30, "Starting"),
        ("RUNNING", 26, 7, 6, "cpp", 30 + int(60 * 6 / 26), "Running test 7 of 26"),
        ("RUNNING", 10, 10, 10, "python", 90, "Finishing"),
        ("JUDGING", 10, 10, 10, "python", 95, "Checking answers"),
    ],
)
def test_percent_and_message(client, stage, total, started, completed, language, percent, message):
    make_submission(language=language)
    set_progress("s1", 5, stage, total, started, completed)

    body = get(client, "s1", since=0).json()

    assert body["percent"] == percent and body["message"] == message


def test_progress_only_ever_moves_forward():
    scale = [
        progress.percent("QUEUED", None, 0),
        progress.percent("PREPARING", 8, 0),
        *[progress.percent("RUNNING", 8, c) for c in range(0, 9)],
        progress.percent("JUDGING", 8, 8),
        progress.percent("DONE", 8, 8),
    ]
    assert scale == sorted(scale)
    assert scale[0] > 0 and scale[-1] == 100


# --- finishing ---


def test_a_finished_submission_returns_the_verdict_with_the_progress(client):
    make_submission(status=SubmissionStatus.PASSED, result="All tests passed", total_tests=26, passed_tests=26,
                    time_taken_ms=40, memory_used="3.34MB")
    set_progress("s1", 9, "DONE", total=26, started=26, completed=26)

    body = get(client, "s1", since=0).json()

    assert body["terminal"] is True and body["percent"] == 100 and body["status"] == "PASSED"
    assert body["result"]["passedTests"] == 26 and body["result"]["totalTests"] == 26
    assert body["result"]["timeTakenMs"] == 40 and body["result"]["memoryUsed"] == "3.34MB"
    assert "code" in body["result"]  # the owner's own submission


def test_a_submission_already_judged_before_the_client_connected(client):
    make_submission(status=SubmissionStatus.FAILED, result="Test Case 2 Failed: Hidden Test Case",
                    total_tests=26, passed_tests=13)

    body = get(client, "s1", since=0).json()

    assert body["terminal"] is True and body["status"] == "FAILED"
    assert body["result"]["result"].startswith("Test Case 2 Failed")


def test_an_engine_error_ends_the_progress(client):
    make_submission(status=SubmissionStatus.FAILED, result="System error while judging")
    set_progress("s1", 4, "ERROR", total=5, started=2, completed=1)

    body = get(client, "s1", since=0).json()

    assert body["terminal"] is True and body["stage"] == "ERROR" and body["message"] == "Judging failed"


def test_an_engine_without_progress_still_finishes_via_the_plain_status_flag(client):
    make_submission(status=SubmissionStatus.IN_PROGRESS)
    redis_client.setex("s1", 600, "PASSED")  # what the engine has always written

    # the database row is what the verdict is read from, so it must be updated first (as the engine does)
    session = SessionLocal()
    try:
        session.query(Submission).update({"status": SubmissionStatus.PASSED, "total_tests": 3, "passed_tests": 3})
        session.commit()
    finally:
        session.close()

    body = get(client, "s1", since=0, wait=2).json()

    assert body["terminal"] is True and body["status"] == "PASSED" and body["result"]["passedTests"] == 3


def test_no_progress_record_and_still_judging_reports_queued_unchanged(client):
    make_submission()

    body = get(client, "s1", since=0, wait=0).json()

    assert body["stage"] == "QUEUED" and body["changed"] is False and body["terminal"] is False


# --- resources ---


def test_waiting_does_not_hold_a_database_connection(client):
    make_submission()
    set_progress("s1", 1, "QUEUED")
    session = SessionLocal()
    seen = []
    ticks = {"n": 0}

    async def fake_sleep(_seconds):
        seen.append(session.in_transaction())
        ticks["n"] += 1
        if ticks["n"] == 3:
            set_progress("s1", 2, "RUNNING", total=4, started=1)

    class User:
        id, is_admin = USER_ID, False

    try:
        with patch("app.services.submission_service.asyncio.sleep", fake_sleep):
            body = asyncio.run(SubmissionService(session).progress("s1", User, since=1, wait=20))
    finally:
        session.close()

    assert body["stage"] == "RUNNING"
    assert seen == [False, False, False]


def test_submitting_records_the_queued_state(client, created_problem):
    problem_id, _ = created_problem
    with patch("app.services.submission_service.sqs.send_to_queue"):
        sub_id = client.post(
            "/api/v1/problem/submit", headers=bearer(),
            json={"problemId": problem_id, "code": "print(1)", "language": "python"},
        ).json()["submissionId"]

    record = progress.read(sub_id)
    assert record["v"] == 1 and record["stage"] == "QUEUED"
    assert get(client, sub_id).json()["message"] == "Queued"


def test_a_redis_failure_while_queueing_does_not_block_the_submission(client, created_problem):
    problem_id, _ = created_problem
    with patch("app.services.submission_service.sqs.send_to_queue"), patch.object(
        progress, "write", side_effect=ConnectionError("down")
    ):
        response = client.post(
            "/api/v1/problem/submit", headers=bearer(),
            json={"problemId": problem_id, "code": "print(1)", "language": "python"},
        )

    assert response.status_code == 200


# --- verdicts ---


def test_the_verdict_and_failing_test_reach_the_client(client):
    make_submission(status=SubmissionStatus.FAILED, result="Wrong answer on test 3 of 26 (hidden test).",
                    total_tests=26, passed_tests=2, verdict="WRONG_ANSWER", failed_test=3)
    set_progress("s1", 9, "DONE", total=26, started=3, completed=2)

    result = get(client, "s1").json()["result"]

    assert result["verdict"] == "WRONG_ANSWER" and result["failedTest"] == 3
    assert result["result"].startswith("Wrong answer on test 3")


def test_submissions_judged_before_verdicts_existed_have_none(client):
    make_submission(status=SubmissionStatus.PASSED, total_tests=5, passed_tests=5)

    result = get(client, "s1").json()["result"]

    assert result["verdict"] is None and result["failedTest"] is None
