import pytest
from sqlalchemy import inspect, text

from app.core import schema_upgrade
from app.core.checker_modes import CHECKER_MODES
from app.database import SessionLocal, engine
from app.models.problem import Problem
from tests.helpers import bearer

BASE = "/api/v1/problem"


# --- empty code is rejected before it reaches the queue ---


@pytest.mark.parametrize("code", ["", "   ", "\n\t\n"])
def test_submitting_empty_code_is_a_validation_error(client, created_problem, code):
    problem_id, _ = created_problem

    response = client.post(f"{BASE}/submit", headers=bearer(),
                           json={"problemId": problem_id, "code": code, "language": "python"})

    assert response.status_code == 422
    assert "empty" in response.text.lower()


def test_running_empty_code_is_a_validation_error(client):
    response = client.post(f"{BASE}/test", headers=bearer(),
                           json={"code": " ", "language": "python", "problemId": 1, "input": ""})

    assert response.status_code == 422


# --- how a problem's answers are compared ---


def test_a_problem_can_choose_how_its_answers_are_compared(client, problem_payload, admin_header):
    response = client.post(f"{BASE}/addproblem", headers=admin_header,
                           json=problem_payload(checker="tokens_ignore_case"))

    assert response.status_code == 200
    session = SessionLocal()
    try:
        assert session.query(Problem).one().checker == "TOKENS_IGNORE_CASE"
    finally:
        session.close()


def test_a_float_problem_stores_its_tolerance(client, problem_payload, admin_header):
    client.post(f"{BASE}/addproblem", headers=admin_header,
                json=problem_payload(checker="FLOAT", checkerTolerance=1e-4))

    session = SessionLocal()
    try:
        problem = session.query(Problem).one()
        assert (problem.checker, problem.checker_tolerance) == ("FLOAT", 1e-4)
    finally:
        session.close()


def test_a_problem_without_a_checker_keeps_the_default(client, problem_payload, admin_header):
    client.post(f"{BASE}/addproblem", headers=admin_header, json=problem_payload())

    session = SessionLocal()
    try:
        assert session.query(Problem).one().checker is None
    finally:
        session.close()


@pytest.mark.parametrize("payload", [{"checker": "MAGIC"}, {"checker": "FLOAT", "checkerTolerance": 0},
                                     {"checker": "FLOAT", "checkerTolerance": -1}])
def test_invalid_checker_settings_are_rejected(client, problem_payload, admin_header, payload):
    response = client.post(f"{BASE}/addproblem", headers=admin_header, json=problem_payload(**payload))

    assert response.status_code == 422


def test_every_checker_mode_is_accepted(client, problem_payload, admin_header):
    for mode in CHECKER_MODES:
        response = client.post(f"{BASE}/addproblem", headers=admin_header, json=problem_payload(checker=mode))
        assert response.status_code == 200, mode


# --- columns added after the tables were first created ---


def test_the_schema_upgrade_adds_missing_columns_and_can_run_again():
    with engine.begin() as connection:
        connection.execute(text("ALTER TABLE submissions DROP COLUMN IF EXISTS verdict"))
        connection.execute(text("ALTER TABLE problems DROP COLUMN IF EXISTS checker_tolerance"))

    schema_upgrade.ensure_columns(engine)
    schema_upgrade.ensure_columns(engine)  # idempotent

    columns = {t: {c["name"] for c in inspect(engine).get_columns(t)} for t in ("submissions", "problems")}
    assert {"verdict", "failed_test"} <= columns["submissions"]
    assert {"checker", "checker_tolerance"} <= columns["problems"]


def test_a_failing_alter_does_not_stop_the_service_starting():
    class BrokenEngine:
        def begin(self):
            raise RuntimeError("no permission")

    schema_upgrade.ensure_columns(BrokenEngine())  # must not raise
