import datetime
from unittest.mock import patch

from app.database import SessionLocal, redis_client
from app.models.problem import Submission, SubmissionStatus, TestAttempt, TestInvite
from tests.helpers import other_bearer

ADMIN = "/api/v1/problem/admin/tests"
TESTS = "/api/v1/problem/tests"


def body(**overrides):
    payload = {
        "title": "Weekly round",
        "visibility": "PUBLIC",
        "selectionMode": "ALL_PROBLEMS",
        "problemCount": 2,
    }
    payload.update(overrides)
    return payload


def three_problems(add_problem):
    return [add_problem(title=f"P{i}") for i in range(3)]


def make_private(client, admin_header, pool, usernames=("tester",), **overrides):
    response = client.post(
        ADMIN,
        json=body(
            visibility="PRIVATE",
            selectionMode="POOL",
            poolProblemIds=pool,
            problemCount=2,
            invites={"usernames": list(usernames), **overrides},
        ),
        headers=admin_header,
    )
    assert response.status_code == 201, response.text
    return response.json()


# --- who may build tests ---


def test_creating_a_test_requires_an_admin(client, auth_header):
    assert client.post(ADMIN, json=body()).status_code == 401
    assert client.post(ADMIN, json=body(), headers=auth_header).status_code == 403
    assert client.get(ADMIN, headers=auth_header).status_code == 403


def test_an_api_key_cannot_create_a_test(client, admin_header):
    # admin actions need a signed-in session; covered for problems already, here only the status
    assert client.post(ADMIN, json=body(), headers={"X-API-Key": "cdr_nope"}).status_code == 401


# --- validation ---


def test_pool_test_needs_a_pool_at_least_as_big_as_the_count(client, admin_header, add_problem):
    ids = three_problems(add_problem)
    pool = body(selectionMode="POOL", poolProblemIds=ids[:1], problemCount=2)
    assert client.post(ADMIN, json=pool, headers=admin_header).status_code == 422
    missing = body(selectionMode="POOL", poolProblemIds=[ids[0], 999999], problemCount=1)
    assert client.post(ADMIN, json=missing, headers=admin_header).status_code == 400


def test_cannot_ask_for_more_problems_than_exist(client, admin_header, add_problem):
    add_problem()
    assert client.post(ADMIN, json=body(problemCount=5), headers=admin_header).status_code == 400


# --- public tests ---


def test_public_test_draws_random_problems_and_can_be_resumed_not_retaken(client, admin_header, auth_header, add_problem):
    three_problems(add_problem)
    created = client.post(ADMIN, json=body(durationMinutes=30), headers=admin_header).json()["test"]

    assert [t["id"] for t in client.get(f"{TESTS}/public", headers=auth_header).json()] == [created["id"]]

    first = client.post(f"{TESTS}/public/{created['id']}/start", headers=auth_header)
    assert first.status_code == 200, first.text
    attempt = first.json()
    assert len(attempt["problems"]) == 2
    assert 0 < attempt["secondsRemaining"] <= 30 * 60

    again = client.post(f"{TESTS}/public/{created['id']}/start", headers=auth_header).json()
    assert again["attemptId"] == attempt["attemptId"]
    assert [p["id"] for p in again["problems"]] == [p["id"] for p in attempt["problems"]]

    client.post(f"{TESTS}/attempts/{attempt['attemptId']}/finish", headers=auth_header)
    assert client.post(f"{TESTS}/public/{created['id']}/start", headers=auth_header).status_code == 409


def test_pool_test_only_uses_the_pool(client, admin_header, auth_header, add_problem):
    ids = three_problems(add_problem)
    created = client.post(
        ADMIN, json=body(selectionMode="POOL", poolProblemIds=ids[:2], problemCount=2), headers=admin_header
    ).json()["test"]
    attempt = client.post(f"{TESTS}/public/{created['id']}/start", headers=auth_header).json()
    assert sorted(p["id"] for p in attempt["problems"]) == sorted(ids[:2])


def test_a_deactivated_test_cannot_be_started(client, admin_header, auth_header, add_problem):
    three_problems(add_problem)
    created = client.post(ADMIN, json=body(), headers=admin_header).json()["test"]
    client.patch(f"{ADMIN}/{created['id']}", json={"isActive": False}, headers=admin_header)
    assert client.get(f"{TESTS}/public", headers=auth_header).json() == []
    assert client.post(f"{TESTS}/public/{created['id']}/start", headers=auth_header).status_code == 404


def test_private_tests_are_not_listed_or_startable_as_public(client, admin_header, auth_header, add_problem):
    ids = three_problems(add_problem)
    test_id = make_private(client, admin_header, ids)["test"]["id"]
    assert client.get(f"{TESTS}/public", headers=auth_header).json() == []
    assert client.post(f"{TESTS}/public/{test_id}/start", headers=auth_header).status_code == 404


def test_a_timed_attempt_ends_when_time_is_up(client, admin_header, auth_header, add_problem):
    three_problems(add_problem)
    created = client.post(ADMIN, json=body(durationMinutes=5), headers=admin_header).json()["test"]
    attempt = client.post(f"{TESTS}/public/{created['id']}/start", headers=auth_header).json()

    session = SessionLocal()
    try:
        row = session.query(TestAttempt).filter(TestAttempt.id == attempt["attemptId"]).one()
        row.expires_at = datetime.datetime.utcnow() - datetime.timedelta(seconds=1)
        session.commit()
    finally:
        session.close()

    now = client.get(f"{TESTS}/attempts/{attempt['attemptId']}", headers=auth_header).json()
    assert now["finished"] is True and now["secondsRemaining"] == 0
    assert client.post(f"{TESTS}/public/{created['id']}/start", headers=auth_header).status_code == 409


# --- invite links ---


def test_invite_link_belongs_to_the_invited_user(client, admin_header, auth_header, add_problem):
    ids = three_problems(add_problem)
    made = make_private(client, admin_header, ids, usernames=["tester", "nobody_here"])
    assert made["unknownUsernames"] == ["nobody_here"]
    invite = made["test"]["inviteList"][0]
    assert invite["username"] == "tester" and invite["path"] == f"/test/{invite['token']}"

    start = f"{TESTS}/invite/{invite['token']}/start"
    assert client.post(start).status_code == 401
    assert client.post(start, headers=other_bearer()).status_code == 403
    assert client.post(f"{TESTS}/invite/not-a-token/start", headers=auth_header).status_code == 404

    ok = client.post(start, headers=auth_header)
    assert ok.status_code == 200, ok.text
    assert sorted(p["id"] for p in ok.json()["problems"]) and len(ok.json()["problems"]) == 2


def test_single_use_link_resumes_until_finished_then_dies(client, admin_header, auth_header, add_problem):
    ids = three_problems(add_problem)
    token = make_private(client, admin_header, ids)["test"]["inviteList"][0]["token"]
    start = f"{TESTS}/invite/{token}/start"

    first = client.post(start, headers=auth_header).json()
    assert client.post(start, headers=auth_header).json()["attemptId"] == first["attemptId"]

    client.post(f"{TESTS}/attempts/{first['attemptId']}/finish", headers=auth_header)
    assert client.post(start, headers=auth_header).status_code == 410

    detail = client.get(f"{ADMIN}/{first['testId']}", headers=admin_header).json()
    assert detail["inviteList"][0]["used"] is True and detail["attempts"] == 1


def test_reusable_link_allows_a_new_attempt_after_finishing(client, admin_header, auth_header, add_problem):
    ids = three_problems(add_problem)
    token = make_private(client, admin_header, ids, singleUse=False)["test"]["inviteList"][0]["token"]
    start = f"{TESTS}/invite/{token}/start"

    first = client.post(start, headers=auth_header).json()
    client.post(f"{TESTS}/attempts/{first['attemptId']}/finish", headers=auth_header)
    second = client.post(start, headers=auth_header)
    assert second.status_code == 200 and second.json()["attemptId"] != first["attemptId"]


def test_revoked_and_expired_links_stop_working(client, admin_header, auth_header, add_problem):
    ids = three_problems(add_problem)
    made = make_private(client, admin_header, ids, usernames=["tester", "someone_else"])
    mine, theirs = made["test"]["inviteList"]
    test_id = made["test"]["id"]

    assert client.delete(f"{ADMIN}/{test_id}/invites/{mine['id']}", headers=admin_header).status_code == 204
    assert client.post(f"{TESTS}/invite/{mine['token']}/start", headers=auth_header).status_code == 410

    session = SessionLocal()
    try:
        row = session.query(TestInvite).filter(TestInvite.id == theirs["id"]).one()
        row.expires_at = datetime.datetime.utcnow() - datetime.timedelta(minutes=1)
        session.commit()
    finally:
        session.close()
    assert client.post(f"{TESTS}/invite/{theirs['token']}/start", headers=other_bearer()).status_code == 410


def test_more_invites_can_be_added_later(client, admin_header, add_problem):
    ids = three_problems(add_problem)
    test_id = make_private(client, admin_header, ids)["test"]["id"]
    added = client.post(f"{ADMIN}/{test_id}/invites", json={"usernames": ["someone_else"]}, headers=admin_header)
    assert added.status_code == 201
    assert len(added.json()["test"]["inviteList"]) == 2


# --- attempts are private ---


def test_attempts_are_only_visible_to_their_owner_and_admins(client, admin_header, auth_header, add_problem):
    three_problems(add_problem)
    created = client.post(ADMIN, json=body(), headers=admin_header).json()["test"]
    attempt = client.post(f"{TESTS}/public/{created['id']}/start", headers=auth_header).json()
    url = f"{TESTS}/attempts/{attempt['attemptId']}"

    assert client.get(url, headers=auth_header).status_code == 200
    assert client.get(url, headers=admin_header).status_code == 200
    assert client.get(url, headers=other_bearer()).status_code == 404
    assert client.post(f"{url}/finish", headers=other_bearer()).status_code == 404


def test_deleting_a_test_removes_its_links_and_attempts(client, admin_header, auth_header, add_problem):
    ids = three_problems(add_problem)
    made = make_private(client, admin_header, ids)["test"]
    token = made["inviteList"][0]["token"]
    client.post(f"{TESTS}/invite/{token}/start", headers=auth_header)

    assert client.delete(f"{ADMIN}/{made['id']}", headers=admin_header).status_code == 204
    assert client.post(f"{TESTS}/invite/{token}/start", headers=auth_header).status_code == 404
    assert client.get(ADMIN, headers=admin_header).json() == []


# --- submissions inside a test ---

SUBMIT = "/api/v1/problem/submit"


def submit(client, headers, attempt_id, problem_id):
    # users may submit once every 10 seconds; these tests are about the attempt checks, not that limit
    for key in redis_client.scan_iter("rl:submit:*"):
        redis_client.delete(key)
    with patch("app.services.submission_service.sqs.send_to_queue"):
        return client.post(
            SUBMIT,
            json={"problemId": problem_id, "code": "print(1)", "language": "python", "attemptId": attempt_id},
            headers=headers,
        )


def start_public(client, admin_header, auth_header, add_problem, **overrides):
    three_problems(add_problem)
    test = client.post(ADMIN, json=body(**overrides), headers=admin_header).json()["test"]
    return test, client.post(f"{TESTS}/public/{test['id']}/start", headers=auth_header).json()


def mark_passed(submission_id):
    session = SessionLocal()
    try:
        row = session.query(Submission).filter(Submission.submission_id == submission_id).one()
        row.status = SubmissionStatus.PASSED
        session.commit()
    finally:
        session.close()


def test_a_submission_inside_a_test_is_tied_to_the_attempt(client, admin_header, auth_header, add_problem):
    _, attempt = start_public(client, admin_header, auth_header, add_problem)
    pid = attempt["problems"][0]["id"]

    response = submit(client, auth_header, attempt["attemptId"], pid)
    assert response.status_code == 200, response.text

    session = SessionLocal()
    try:
        row = session.query(Submission).filter(Submission.submission_id == response.json()["submissionId"]).one()
        assert row.attempt_id == attempt["attemptId"]
    finally:
        session.close()


def test_submissions_are_refused_for_other_problems_other_users_and_finished_attempts(
    client, admin_header, auth_header, add_problem
):
    test, attempt = start_public(client, admin_header, auth_header, add_problem, problemCount=1)
    drawn = attempt["problems"][0]["id"]
    outside = next(p for p in client.get("/api/v1/problem/problems").json() if p["id"] != drawn)["id"]

    assert submit(client, auth_header, attempt["attemptId"], outside).status_code == 400
    assert submit(client, other_bearer(), attempt["attemptId"], drawn).status_code == 404
    assert submit(client, auth_header, 999999, drawn).status_code == 404

    client.post(f"{TESTS}/attempts/{attempt['attemptId']}/finish", headers=auth_header)
    assert submit(client, auth_header, attempt["attemptId"], drawn).status_code == 409


def test_no_submissions_after_time_is_up(client, admin_header, auth_header, add_problem):
    _, attempt = start_public(client, admin_header, auth_header, add_problem, durationMinutes=5)
    session = SessionLocal()
    try:
        row = session.query(TestAttempt).filter(TestAttempt.id == attempt["attemptId"]).one()
        row.expires_at = datetime.datetime.utcnow() - datetime.timedelta(seconds=1)
        session.commit()
    finally:
        session.close()

    assert submit(client, auth_header, attempt["attemptId"], attempt["problems"][0]["id"]).status_code == 409


def test_accepted_problems_show_as_solved_and_in_the_admin_results(client, admin_header, auth_header, add_problem):
    test, attempt = start_public(client, admin_header, auth_header, add_problem)
    first, second = (p["id"] for p in attempt["problems"])

    wrong = submit(client, auth_header, attempt["attemptId"], first).json()["submissionId"]
    right = submit(client, auth_header, attempt["attemptId"], first).json()["submissionId"]
    submit(client, auth_header, attempt["attemptId"], second)
    mark_passed(right)
    assert wrong != right

    seen = client.get(f"{TESTS}/attempts/{attempt['attemptId']}", headers=auth_header).json()
    assert seen["solved"] == [first]

    rows = client.get(f"{ADMIN}/{test['id']}/results", headers=admin_header).json()
    assert len(rows) == 1 and rows[0]["username"] == "tester"
    assert (rows[0]["solvedCount"], rows[0]["total"]) == (1, 2)
    by_id = {p["id"]: p for p in rows[0]["problems"]}
    assert by_id[first]["solved"] is True and by_id[first]["submissions"] == 2
    assert by_id[second]["solved"] is False and by_id[second]["submissions"] == 1

    assert client.get(f"{ADMIN}/{test['id']}/results", headers=auth_header).status_code == 403


# --- difficulty per problem ---


def levelled(add_problem):
    ids = {}
    for level, n in (("Easy", 2), ("Medium", 2), ("Hard", 1)):
        for i in range(n):
            ids.setdefault(level, []).append(add_problem(title=f"{level}{i}", difficulty=level))
    return ids


def test_each_problem_slot_gets_its_requested_difficulty(client, admin_header, auth_header, add_problem):
    ids = levelled(add_problem)
    created = client.post(
        ADMIN,
        json=body(problemCount=3, slotDifficulties=["HARD", "EASY", "ANY"]),
        headers=admin_header,
    )
    assert created.status_code == 201, created.text
    assert created.json()["test"]["slotDifficulties"] == ["HARD", "EASY", "ANY"]

    attempt = client.post(f"{TESTS}/public/{created.json()['test']['id']}/start", headers=auth_header).json()
    drawn = [p["id"] for p in attempt["problems"]]
    assert drawn[0] == ids["Hard"][0]
    assert drawn[1] in ids["Easy"]
    assert len(set(drawn)) == 3
    assert all("difficulty" not in p for p in attempt["problems"])


def test_a_difficulty_mix_the_problems_cannot_fill_is_refused(client, admin_header, add_problem):
    levelled(add_problem)
    too_many_hard = body(problemCount=2, slotDifficulties=["HARD", "HARD"])
    assert client.post(ADMIN, json=too_many_hard, headers=admin_header).status_code == 400
    wrong_length = body(problemCount=2, slotDifficulties=["EASY"])
    assert client.post(ADMIN, json=wrong_length, headers=admin_header).status_code == 422


def test_difficulty_slots_work_within_a_pool(client, admin_header, auth_header, add_problem):
    ids = levelled(add_problem)
    pool = ids["Easy"] + ids["Hard"]
    ok = client.post(
        ADMIN,
        json=body(selectionMode="POOL", poolProblemIds=pool, problemCount=2, slotDifficulties=["EASY", "EASY"]),
        headers=admin_header,
    )
    assert ok.status_code == 201
    no_medium = body(selectionMode="POOL", poolProblemIds=pool, problemCount=1, slotDifficulties=["MEDIUM"])
    assert client.post(ADMIN, json=no_medium, headers=admin_header).status_code == 400


# --- active attempts ---


def test_active_attempts_lists_only_running_ones(client, admin_header, auth_header, add_problem):
    test, attempt = start_public(client, admin_header, auth_header, add_problem, durationMinutes=30)
    active = client.get(f"{TESTS}/attempts/active", headers=auth_header).json()
    assert [a["attemptId"] for a in active] == [attempt["attemptId"]]
    assert active[0]["total"] == 2 and active[0]["solved"] == 0 and active[0]["secondsRemaining"] > 0
    assert client.get(f"{TESTS}/attempts/active", headers=other_bearer()).json() == []

    client.post(f"{TESTS}/attempts/{attempt['attemptId']}/finish", headers=auth_header)
    assert client.get(f"{TESTS}/attempts/active", headers=auth_header).json() == []


# --- test submissions stay out of the real problems ---


def test_test_submissions_do_not_show_up_on_the_problem(client, admin_header, auth_header, add_problem):
    _, attempt = start_public(client, admin_header, auth_header, add_problem)
    pid = attempt["problems"][0]["id"]
    assert submit(client, auth_header, attempt["attemptId"], pid).status_code == 200

    assert client.get(f"/api/v1/problem/submissions/subuser/{pid}", headers=auth_header).json() == []
    assert client.get("/api/v1/problem/recent", headers=auth_header).json() == []
    found = client.get("/api/v1/problem/search?search=P").json()["content"]
    assert all(p["submissions"] == 0 for p in found)


# --- one attempt or many ---


def test_public_test_with_multiple_attempts_can_be_retaken(client, admin_header, auth_header, add_problem):
    test, attempt = start_public(client, admin_header, auth_header, add_problem, multipleAttempts=True)
    assert test["multipleAttempts"] is True
    client.post(f"{TESTS}/attempts/{attempt['attemptId']}/finish", headers=auth_header)

    again = client.post(f"{TESTS}/public/{test['id']}/start", headers=auth_header)
    assert again.status_code == 200 and again.json()["attemptId"] != attempt["attemptId"]


def test_invite_links_follow_the_tests_attempt_setting(client, admin_header, add_problem):
    ids = three_problems(add_problem)
    many = client.post(
        ADMIN,
        json=body(visibility="PRIVATE", selectionMode="POOL", poolProblemIds=ids, multipleAttempts=True,
                  invites={"usernames": ["tester"]}),
        headers=admin_header,
    ).json()["test"]
    assert many["inviteList"][0]["singleUse"] is False

    added = client.post(f"{ADMIN}/{many['id']}/invites", json={"usernames": ["someone_else"]}, headers=admin_header)
    assert added.json()["test"]["inviteList"][1]["singleUse"] is False

    once = make_private(client, admin_header, ids)["test"]
    assert once["multipleAttempts"] is False and once["inviteList"][0]["singleUse"] is True


# --- the caller's standing in the public list, and the clock ---


def my_status(client, headers, test_id):
    return next(t for t in client.get(f"{TESTS}/public", headers=headers).json() if t["id"] == test_id)


def test_public_list_shows_where_the_caller_stands(client, admin_header, auth_header, add_problem):
    test, attempt = start_public(client, admin_header, auth_header, add_problem)
    assert my_status(client, auth_header, test["id"])["myStatus"] == "IN_PROGRESS"
    assert my_status(client, auth_header, test["id"])["myAttemptId"] == attempt["attemptId"]
    assert my_status(client, other_bearer(), test["id"])["myStatus"] == "NOT_STARTED"

    client.post(f"{TESTS}/attempts/{attempt['attemptId']}/finish", headers=auth_header)
    assert my_status(client, auth_header, test["id"])["myStatus"] == "DONE"


def test_a_finished_retakeable_test_shows_as_retake(client, admin_header, auth_header, add_problem):
    test, attempt = start_public(client, admin_header, auth_header, add_problem, multipleAttempts=True)
    client.post(f"{TESTS}/attempts/{attempt['attemptId']}/finish", headers=auth_header)
    assert my_status(client, auth_header, test["id"])["myStatus"] == "RETAKE"


def test_untimed_attempts_report_elapsed_time(client, admin_header, auth_header, add_problem):
    _, attempt = start_public(client, admin_header, auth_header, add_problem)
    assert attempt["secondsRemaining"] is None
    assert attempt["secondsElapsed"] >= 0
