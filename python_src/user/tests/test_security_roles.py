import pytest

from app.core import security
from app.models.user import User
from app.services.login_throttle import MAX_FAILURES

BASE = "/api/v1/user"


def register(client, payload):
    return client.post(f"{BASE}/register", json=payload)


def login(client, email, password):
    return client.post(f"{BASE}/login", json={"email": email, "password": password})


def bearer(token):
    return {"Authorization": f"Bearer {token}"}


def make_admin(db, email):
    db.query(User).filter(User.email == email).update({"role": "ADMIN"})
    db.commit()


@pytest.fixture
def user_token(client, new_user):
    register(client, new_user)
    return login(client, new_user["email"], new_user["password"]).json()["token"]


@pytest.fixture
def admin(client, db, new_user):
    """An admin account: returns (user payload, token)."""
    register(client, new_user)
    make_admin(db, new_user["email"])
    token = login(client, new_user["email"], new_user["password"]).json()["token"]
    return new_user, token


# --- responses never carry the password hash ---


def test_register_response_has_no_password(client, new_user):
    body = register(client, new_user).json()

    assert "password" not in body
    assert "$2" not in str(body)


def test_get_user_has_no_password(client, user_token):
    body = client.get(f"{BASE}/user", headers=bearer(user_token)).json()

    assert "password" not in body
    assert body["role"] == "USER"
    assert body["dailyStreak"] == 0


# --- registration rules ---


def test_registration_cannot_choose_a_role(client, new_user):
    response = register(client, {**new_user, "role": "ADMIN"})

    assert response.json()["role"] == "USER"


@pytest.mark.parametrize("name", ["admin", "Admin", "ADMINISTRATOR", "root"])
def test_reserved_usernames_are_rejected(client, new_user, name):
    assert register(client, {**new_user, "username": name}).status_code == 422


def test_username_uniqueness_ignores_case(client, new_user):
    register(client, {**new_user, "username": "Mixed_Case"})

    other = {**new_user, "username": "mixed_case", "email": "other_" + new_user["email"]}

    assert register(client, other).status_code == 400


def test_weak_or_oversized_passwords_are_rejected(client, new_user):
    assert register(client, {**new_user, "password": "short"}).status_code == 422
    assert register(client, {**new_user, "password": "x" * 73}).status_code == 422


def test_email_is_case_insensitive(client, new_user):
    register(client, {**new_user, "email": new_user["email"].upper()})

    assert login(client, new_user["email"], new_user["password"]).status_code == 200


# --- tokens ---


def test_token_lifetime_is_capped(monkeypatch):
    monkeypatch.setenv("JWT_EXPIRY", "86400000")  # milliseconds, as an older deployment set it

    assert security.token_minutes() == security.MAX_TOKEN_MINUTES


def test_token_for_demoted_or_unknown_role_gets_no_privilege(client, user_token):
    # a "role" claim inside a token is ignored: only the database decides
    forged = security.create_access_token({"sub": "1", "username": "x", "role": "ADMIN"})

    assert client.get(f"{BASE}/admin/users", headers=bearer(forged)).status_code == 401
    assert client.get(f"{BASE}/admin/users", headers=bearer(user_token)).status_code == 403


# --- login throttle ---


def test_login_is_throttled_after_repeated_failures(client, new_user):
    register(client, new_user)

    for _ in range(MAX_FAILURES):
        assert login(client, new_user["email"], "wrong-password").status_code == 401

    blocked = login(client, new_user["email"], new_user["password"])
    assert blocked.status_code == 429


def test_successful_login_resets_the_failure_count(client, new_user):
    register(client, new_user)
    for _ in range(MAX_FAILURES - 1):
        login(client, new_user["email"], "wrong-password")

    assert login(client, new_user["email"], new_user["password"]).status_code == 200
    for _ in range(MAX_FAILURES - 1):
        assert login(client, new_user["email"], "wrong-password").status_code == 401


# --- admin routes ---


def test_admin_routes_require_authentication(client):
    assert client.get(f"{BASE}/admin/users").status_code == 401


def test_admin_can_list_users(client, admin):
    _, token = admin

    response = client.get(f"{BASE}/admin/users", headers=bearer(token))

    assert response.status_code == 200
    assert all("password" not in u for u in response.json())


def test_admin_can_promote_and_demote_a_user(client, admin, new_user):
    _, admin_token = admin
    other = {**new_user, "username": "plain_user", "email": "plain_" + new_user["email"]}
    other_id = register(client, other).json()["id"]
    other_token = login(client, other["email"], other["password"]).json()["token"]

    # warm the cache with the USER role, then promote: the change must be visible at once
    client.get(f"{BASE}/user", headers=bearer(other_token))
    promoted = client.patch(
        f"{BASE}/admin/users/{other_id}/role", headers=bearer(admin_token), json={"role": "admin"}
    )
    assert promoted.json()["role"] == "ADMIN"
    assert client.get(f"{BASE}/user", headers=bearer(other_token)).json()["role"] == "ADMIN"
    assert client.get(f"{BASE}/admin/users", headers=bearer(other_token)).status_code == 200

    client.patch(f"{BASE}/admin/users/{other_id}/role", headers=bearer(admin_token), json={"role": "USER"})
    assert client.get(f"{BASE}/admin/users", headers=bearer(other_token)).status_code == 403


def test_admin_cannot_remove_their_own_role(client, admin):
    payload, token = admin
    my_id = client.get(f"{BASE}/user", headers=bearer(token)).json()["id"]

    response = client.patch(
        f"{BASE}/admin/users/{my_id}/role", headers=bearer(token), json={"role": "USER"}
    )

    assert response.status_code == 400


def test_regular_user_cannot_change_roles(client, user_token):
    response = client.patch(f"{BASE}/admin/users/1/role", headers=bearer(user_token), json={"role": "ADMIN"})

    assert response.status_code == 403


def test_invalid_role_is_rejected(client, admin):
    _, token = admin

    response = client.patch(f"{BASE}/admin/users/1/role", headers=bearer(token), json={"role": "ROOT"})

    assert response.status_code == 422


# --- change password ---


def test_change_password(client, new_user, user_token):
    response = client.post(
        f"{BASE}/change-password",
        headers=bearer(user_token),
        json={"currentPassword": new_user["password"], "newPassword": "a-brand-new-pass"},
    )

    assert response.status_code == 200
    assert login(client, new_user["email"], new_user["password"]).status_code == 401
    assert login(client, new_user["email"], "a-brand-new-pass").status_code == 200


def test_change_password_needs_the_current_password(client, user_token):
    response = client.post(
        f"{BASE}/change-password",
        headers=bearer(user_token),
        json={"currentPassword": "not-it", "newPassword": "a-brand-new-pass"},
    )

    assert response.status_code == 400


def test_change_password_requires_authentication(client):
    response = client.post(
        f"{BASE}/change-password", json={"currentPassword": "x", "newPassword": "a-brand-new-pass"}
    )

    assert response.status_code == 401
