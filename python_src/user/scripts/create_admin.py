"""Create the first admin, or promote an existing user to admin.

Run from python_src/user with the service's environment loaded (DB_*, PASSWORD, JWT_SECRET ...):

    python -m scripts.create_admin --email admin@codear.com --username codear_admin --name "Codear Admin"

A strong random password is generated and printed once. Set ADMIN_PASSWORD to choose one instead.
If the email already exists, that user is promoted and their password is left alone.
"""
import argparse
import os
import secrets
import string
import sys

from sqlalchemy import func

from app.core import security
from app.database import SessionLocal
from app.models.user import User
from app.schemas.user_schema import USERNAME_RE


def generate_password(length: int = 20) -> str:
    alphabet = string.ascii_letters + string.digits + "-_.!@#%^*"
    while True:
        pwd = "".join(secrets.choice(alphabet) for _ in range(length))
        if any(c.islower() for c in pwd) and any(c.isupper() for c in pwd) and any(c.isdigit() for c in pwd):
            return pwd


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--email", required=True)
    parser.add_argument("--username", required=True)
    parser.add_argument("--name", required=True)
    args = parser.parse_args()

    email = args.email.strip().lower()
    username = args.username.strip()

    db = SessionLocal()
    try:
        existing = db.query(User).filter(func.lower(User.email) == email).first()
        if existing:
            existing.role = security.ROLE_ADMIN
            db.commit()
            print(f"Promoted existing user {existing.username} <{existing.email}> to ADMIN. Password unchanged.")
            return 0

        # The reserved-name rule exists to stop public sign-ups impersonating staff;
        # an operator creating an admin may use such a name on purpose, so only the shape is checked.
        if not USERNAME_RE.match(username):
            print("Username must be 3-32 characters: letters, numbers, dot, dash or underscore", file=sys.stderr)
            return 1
        if db.query(User).filter(func.lower(User.username) == username.lower()).first():
            print(f"Username '{username}' is already taken by a different email.", file=sys.stderr)
            return 1

        password = os.getenv("ADMIN_PASSWORD") or generate_password()
        generated = not os.getenv("ADMIN_PASSWORD")

        db.add(
            User(
                username=username,
                name=args.name.strip(),
                email=email,
                password=security.get_password_hash(password),
                role=security.ROLE_ADMIN,
                daily_streak=0,
                problem_solved_total=0,
            )
        )
        db.commit()

        print(f"Created ADMIN {username} <{email}>")
        if generated:
            print(f"Initial password (shown once, change it after first login): {password}")
        return 0
    finally:
        db.close()


if __name__ == "__main__":
    sys.exit(main())
