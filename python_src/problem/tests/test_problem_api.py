BASE = "/api/v1/problem"


def test_health_check(client):
    response = client.get(f"{BASE}/health-check")

    assert response.status_code == 200


def test_add_problem_persists_it(client, problem_payload):
    payload = problem_payload()

    response = client.post(f"{BASE}/addproblem", json=payload)

    assert response.status_code == 200
    body = response.json()
    assert body["id"] > 0
    assert body["title"] == payload["title"]
    assert body["difficulty"] == payload["difficulty"]
    assert body["tags"] == payload["tags"]


def test_get_problem_by_id_hides_non_sample_tests(client, created_problem):
    problem_id, payload = created_problem

    response = client.get(f"{BASE}/problem/{problem_id}")

    assert response.status_code == 200
    body = response.json()
    assert body["title"] == payload["title"]
    assert body["inputDescription"] == payload["inputDescription"]
    assert body["timeLimitMs"] == payload["timeLimitMs"]
    # Only the sample test case is exposed, the hidden one is not.
    assert len(body["testCases"]) == 1
    assert body["testCases"][0]["isSample"] is True
    assert body["testCases"][0]["output"] == "0 1"


def test_get_problem_by_id_is_served_from_cache_on_repeat(client, created_problem):
    problem_id, _ = created_problem

    first = client.get(f"{BASE}/problem/{problem_id}").json()
    second = client.get(f"{BASE}/problem/{problem_id}").json()

    assert first == second


def test_get_problem_by_id_404s_when_missing(client):
    response = client.get(f"{BASE}/problem/424242")

    assert response.status_code == 404


def test_list_problems_returns_summaries(client, add_problem):
    add_problem(title="Alpha")
    add_problem(title="Beta")

    response = client.get(f"{BASE}/problems")

    assert response.status_code == 200
    titles = sorted(p["title"] for p in response.json())
    assert titles == ["Alpha", "Beta"]


def test_problem_count_and_tags(client, add_problem):
    add_problem(title="Alpha", tags=["array"])
    add_problem(title="Beta", tags=["graph", "dp"])

    response = client.get(f"{BASE}/problemCntAndTags")

    assert response.status_code == 200
    body = response.json()
    assert body["count"] == 2
    assert body["tags"] == ["array", "dp", "graph"]


def test_delete_problem(client, created_problem):
    problem_id, _ = created_problem

    assert client.delete(f"{BASE}/{problem_id}").status_code == 200
    assert client.get(f"{BASE}/problem/{problem_id}").status_code == 404


def test_delete_problem_404s_when_missing(client):
    assert client.delete(f"{BASE}/424242").status_code == 404


def test_delete_problem_refreshes_the_count(client, created_problem):
    problem_id, _ = created_problem
    assert client.get(f"{BASE}/problemCntAndTags").json()["count"] == 1

    client.delete(f"{BASE}/{problem_id}")

    assert client.get(f"{BASE}/problemCntAndTags").json()["count"] == 0


def test_search_without_filters_returns_everything(client, add_problem):
    add_problem(title="Alpha")
    add_problem(title="Beta")

    body = client.get(f"{BASE}/search").json()

    assert body["totalCount"] == 2
    assert len(body["content"]) == 2


def test_search_filters_by_difficulty(client, add_problem):
    add_problem(title="Easy One", difficulty="easy")
    add_problem(title="Hard One", difficulty="hard")

    body = client.get(f"{BASE}/search", params={"difficulty": "HARD"}).json()

    assert body["totalCount"] == 1
    assert body["content"][0]["title"] == "Hard One"


def test_search_filters_by_tag(client, add_problem):
    add_problem(title="Graph One", tags=["graph"])
    add_problem(title="Array One", tags=["array"])

    body = client.get(f"{BASE}/search", params={"tags": ["graph"]}).json()

    assert body["totalCount"] == 1
    assert body["content"][0]["title"] == "Graph One"


def test_search_matches_full_text(client, add_problem):
    add_problem(title="Dijkstra", description="Shortest path on a weighted graph")
    add_problem(title="Two Sum", description="Find two numbers adding to a target")

    body = client.get(f"{BASE}/search", params={"search": "shortest path"}).json()

    assert body["totalCount"] == 1
    assert body["content"][0]["title"] == "Dijkstra"


def test_search_paginates(client, add_problem):
    add_problem(title="Alpha")
    add_problem(title="Beta")
    add_problem(title="Gamma")

    body = client.get(f"{BASE}/search", params={"page": 0, "size": 2}).json()

    assert body["totalCount"] == 3
    assert body["totalPages"] == 2
    assert len(body["content"]) == 2


def test_recent_problems_is_empty_without_submissions(client, auth_header):
    response = client.get(f"{BASE}/recent", headers=auth_header)

    assert response.status_code == 200
    assert response.json() == []


def test_recent_problems_rejects_invalid_token(client):
    response = client.get(f"{BASE}/recent", headers={"Authorization": "Bearer nope"})

    assert response.status_code == 401


def test_recent_problems_requires_the_header(client):
    assert client.get(f"{BASE}/recent").status_code == 422
