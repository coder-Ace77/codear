from tests.helpers import bearer

BASE = "/api/v1/problem"

# EditorialDTO declares camelCase fields with snake_case aliases, and FastAPI
# serialises responses by alias, so the wire format is snake_case.


def test_editorials_are_empty_for_a_fresh_problem(client, created_problem):
    problem_id, _ = created_problem

    response = client.get(f"{BASE}/{problem_id}/editorial")

    assert response.status_code == 200
    assert response.json() == []


def test_create_editorial(client, created_problem, auth_header):
    problem_id, _ = created_problem

    response = client.post(
        f"{BASE}/{problem_id}/editorial",
        headers=auth_header,
        json={"problemId": problem_id, "title": "Hashmap approach", "content": "..."},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["problem_id"] == problem_id
    assert body["title"] == "Hashmap approach"
    assert body["username"] == "tester"
    assert body["is_admin"] is False
    assert body["upvotes"] == 0


def test_created_editorial_is_listed(client, created_problem, auth_header):
    problem_id, _ = created_problem
    client.post(
        f"{BASE}/{problem_id}/editorial",
        headers=auth_header,
        json={"problemId": problem_id, "title": "Hashmap approach", "content": "..."},
    )

    listed = client.get(f"{BASE}/{problem_id}/editorial").json()

    assert len(listed) == 1
    assert listed[0]["title"] == "Hashmap approach"


def test_create_editorial_rejects_invalid_token(client, created_problem):
    problem_id, _ = created_problem

    response = client.post(
        f"{BASE}/{problem_id}/editorial",
        headers={"Authorization": "Bearer nope"},
        json={"problemId": problem_id, "title": "t", "content": "c"},
    )

    assert response.status_code == 401


def test_editorial_url_id_wins_over_body_id(client, created_problem, auth_header):
    problem_id, _ = created_problem

    response = client.post(
        f"{BASE}/{problem_id}/editorial",
        headers=auth_header,
        json={"problemId": problem_id + 999, "title": "t", "content": "c"},
    )

    assert response.status_code == 200
    assert response.json()["problem_id"] == problem_id


def test_admin_editorials_are_listed_first(client, created_problem):
    problem_id, _ = created_problem
    for username in ("tester", "admin"):
        client.post(
            f"{BASE}/{problem_id}/editorial",
            headers=bearer(username=username),
            json={"problemId": problem_id, "title": username, "content": "c"},
        )

    listed = client.get(f"{BASE}/{problem_id}/editorial").json()

    assert [e["title"] for e in listed] == ["admin", "tester"]
    assert listed[0]["is_admin"] is True
