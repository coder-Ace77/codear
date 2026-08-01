from app.core import security


def register(client, payload):
    return client.post("/api/v1/user/register", json=payload)


def login(client, email, password):
    return client.post("/api/v1/user/login", json={"email": email, "password": password})


def test_health_check(client):
    response = client.get("/api/v1/user/health")

    assert response.status_code == 200
    assert "up and running" in response.json()


def test_register_creates_user(client, new_user):
    response = register(client, new_user)

    assert response.status_code == 200
    body = response.json()
    assert body["id"] > 0
    assert body["username"] == new_user["username"]
    assert body["email"] == new_user["email"]
    assert body["role"] == "USER"


def test_register_rejects_duplicate_email(client, new_user):
    register(client, new_user)

    response = register(client, {**new_user, "username": new_user["username"] + "_2"})

    assert response.status_code == 400
    assert response.json()["detail"] == "Email already in use"


def test_register_rejects_duplicate_username(client, new_user):
    register(client, new_user)

    response = register(client, {**new_user, "email": "other_" + new_user["email"]})

    assert response.status_code == 400
    assert response.json()["detail"] == "Username already taken"


def test_register_rejects_malformed_email(client, new_user):
    response = register(client, {**new_user, "email": "not-an-email"})

    assert response.status_code == 422


def test_login_returns_token_for_valid_credentials(client, new_user):
    user_id = register(client, new_user).json()["id"]

    response = login(client, new_user["email"], new_user["password"])

    assert response.status_code == 200
    token = response.json()["token"]
    assert security.extract_user_id(token) == str(user_id)


def test_login_rejects_wrong_password(client, new_user):
    register(client, new_user)

    response = login(client, new_user["email"], "wrong-password")

    assert response.status_code == 401


def test_login_rejects_unknown_email(client, new_user):
    response = login(client, new_user["email"], new_user["password"])

    assert response.status_code == 401


def test_get_user_returns_the_token_owner(client, new_user):
    register(client, new_user)
    token = login(client, new_user["email"], new_user["password"]).json()["token"]

    response = client.get(
        "/api/v1/user/user", headers={"Authorization": f"Bearer {token}"}
    )

    assert response.status_code == 200
    assert response.json()["email"] == new_user["email"]


def test_get_user_requires_authorization_header(client):
    assert client.get("/api/v1/user/user").status_code == 401


def test_get_user_rejects_non_bearer_header(client):
    response = client.get("/api/v1/user/user", headers={"Authorization": "Basic abc"})

    assert response.status_code == 401


def test_get_user_rejects_invalid_token(client):
    response = client.get(
        "/api/v1/user/user", headers={"Authorization": "Bearer not-a-jwt"}
    )

    assert response.status_code == 401


def test_get_user_404s_for_deleted_user(client):
    token = security.create_access_token({"sub": "999999"})

    response = client.get(
        "/api/v1/user/user", headers={"Authorization": f"Bearer {token}"}
    )

    assert response.status_code == 404


def test_chat_history_requires_authorization(client):
    assert client.get("/api/v1/user/chat/history/p1").status_code == 401
