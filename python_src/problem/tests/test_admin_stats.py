from unittest.mock import patch

from app.database import SessionLocal
from app.models.problem import Submission, SubmissionStatus
from tests.helpers import USER_ID, admin_bearer, bearer

BASE = "/api/v1/problem/admin"


def add_submission(sub_id, status, problem_id, user_id=USER_ID):
    session = SessionLocal()
    try:
        session.add(
            Submission(
                submission_id=sub_id, user_id=user_id, problem_id=problem_id,
                code="SECRET SOLUTION", language="python", status=status,
                result="SECRET RESULT", total_tests=3, passed_tests=3, time_taken_ms=40,
            )
        )
        session.commit()
    finally:
        session.close()


def fake_attrs(waiting=4, in_flight=2, delayed=1):
    return {
        "Attributes": {
            "ApproximateNumberOfMessages": str(waiting),
            "ApproximateNumberOfMessagesNotVisible": str(in_flight),
            "ApproximateNumberOfMessagesDelayed": str(delayed),
        }
    }


def test_stats_are_admin_only(client):
    assert client.get(f"{BASE}/stats").status_code == 401
    assert client.get(f"{BASE}/stats", headers=bearer()).status_code == 403
    assert client.get(f"{BASE}/submissions").status_code == 401
    assert client.get(f"{BASE}/submissions", headers=bearer()).status_code == 403


def test_stats_report_queue_depth_and_volume(client, created_problem):
    problem_id, _ = created_problem
    add_submission("a", SubmissionStatus.PASSED, problem_id)
    add_submission("b", SubmissionStatus.IN_PROGRESS, problem_id)

    with patch("app.services.admin_stats_service.sqs.DEFAULT_QUEUE_URL", "https://sqs/x"), patch(
        "app.services.admin_stats_service.sqs.TEST_QUEUE_URL", "https://sqs/y"
    ), patch("app.services.admin_stats_service.sqs.sqs_client.get_queue_attributes", return_value=fake_attrs()):
        body = client.get(f"{BASE}/stats", headers=admin_bearer()).json()

    assert body["queues"][0] == {"name": "submissions", "configured": True, "waiting": 4, "inFlight": 2, "delayed": 1}
    assert body["submissions"]["total"] == 2
    assert body["submissions"]["lastMinute"] == 2
    assert body["submissions"]["inProgress"] == 1
    assert body["submissions"]["byStatusLast24Hours"] == {"PASSED": 1, "IN_PROGRESS": 1}
    assert body["topSubmittersLastHour"] == [{"userId": USER_ID, "username": "tester", "submissions": 2}]


def test_stats_survive_an_unreadable_queue(client):
    with patch("app.services.admin_stats_service.sqs.DEFAULT_QUEUE_URL", "https://sqs/x"), patch(
        "app.services.admin_stats_service.sqs.TEST_QUEUE_URL", None
    ), patch("app.services.admin_stats_service.sqs.sqs_client.get_queue_attributes", side_effect=RuntimeError("secret arn")):
        response = client.get(f"{BASE}/stats", headers=admin_bearer())

    assert response.status_code == 200
    body = response.json()
    assert body["queues"][0]["error"] == "Could not read queue depth"
    assert "secret" not in response.text
    assert body["queues"][1]["configured"] is False


def test_a_long_running_submission_is_flagged_as_stuck(client, created_problem):
    import datetime

    problem_id, _ = created_problem
    add_submission("old", SubmissionStatus.IN_PROGRESS, problem_id)
    session = SessionLocal()
    try:
        session.query(Submission).update({"submitted_at": datetime.datetime.utcnow() - datetime.timedelta(minutes=10)})
        session.commit()
    finally:
        session.close()

    with patch("app.services.admin_stats_service.sqs.DEFAULT_QUEUE_URL", None), patch(
        "app.services.admin_stats_service.sqs.TEST_QUEUE_URL", None
    ):
        stats = client.get(f"{BASE}/stats", headers=admin_bearer()).json()["submissions"]

    assert stats["stuck"] is True
    assert stats["oldestInProgressSeconds"] >= 600


def test_rate_limit_blocks_are_counted(client, created_problem):
    problem_id, _ = created_problem
    with patch("app.services.submission_service.sqs.send_to_queue"):
        for _ in range(3):
            client.post(
                "/api/v1/problem/submit", headers=bearer(),
                json={"problemId": problem_id, "code": "x", "language": "python"},
            )

    with patch("app.services.admin_stats_service.sqs.DEFAULT_QUEUE_URL", None), patch(
        "app.services.admin_stats_service.sqs.TEST_QUEUE_URL", None
    ):
        stats = client.get(f"{BASE}/stats", headers=admin_bearer()).json()

    assert stats["rateLimit"]["blockedLast5Minutes"] == 2


def test_recent_submissions_never_include_code_or_results(client, created_problem):
    problem_id, _ = created_problem
    add_submission("s1", SubmissionStatus.PASSED, problem_id)

    response = client.get(f"{BASE}/submissions", headers=admin_bearer())

    assert response.status_code == 200
    row = response.json()[0]
    assert row["username"] == "tester"
    assert row["problemTitle"] == "Two Sum"
    assert row["status"] == "PASSED"
    assert "SECRET" not in response.text
    assert "code" not in row and "result" not in row


def test_recent_submissions_limit_is_bounded(client):
    assert client.get(f"{BASE}/submissions?limit=500", headers=admin_bearer()).status_code == 422
    assert client.get(f"{BASE}/submissions?limit=0", headers=admin_bearer()).status_code == 422
