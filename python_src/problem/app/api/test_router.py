from typing import List

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.auth import CurrentUser, get_current_user, require_admin
from app.database import get_db
from app.schemas.test_schema import (
    ActiveAttempt,
    AddInvitesRequest,
    AttemptOut,
    AttemptResult,
    CreatedTest,
    CreateTestRequest,
    PublicTest,
    TestHistory,
    TestDetail,
    TestSummary,
)
from app.services.test_service import TestService

admin_router = APIRouter(prefix="/api/v1/problem/admin/tests")
router = APIRouter(prefix="/api/v1/problem/tests")


class ActiveRequest(BaseModel):
    isActive: bool


# --- admin: build and manage tests ---

@admin_router.post("", response_model=CreatedTest, status_code=201)
def create_test(req: CreateTestRequest, admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return TestService(db).create(req, admin)


@admin_router.get("", response_model=List[TestSummary])
def list_tests(_admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return TestService(db).list_tests()


@admin_router.get("/history", response_model=TestHistory)
def user_history(username: str, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    """Any user's past test attempts, looked up by username."""
    return TestService(db).history_by_username(username)


@admin_router.get("/{test_id}", response_model=TestDetail)
def get_test(test_id: int, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return TestService(db).get_detail(test_id)


@admin_router.patch("/{test_id}", response_model=TestDetail)
def set_active(test_id: int, body: ActiveRequest, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return TestService(db).set_active(test_id, body.isActive)


@admin_router.post("/{test_id}/invites", response_model=CreatedTest, status_code=201)
def add_invites(test_id: int, body: AddInvitesRequest, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return TestService(db).add_invites(test_id, body)


@admin_router.delete("/{test_id}/invites/{invite_id}", status_code=204)
def revoke_invite(test_id: int, invite_id: int, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    TestService(db).revoke_invite(test_id, invite_id)


@admin_router.get("/{test_id}/results", response_model=List[AttemptResult])
def results(test_id: int, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return TestService(db).results(test_id)


@admin_router.delete("/{test_id}", status_code=204)
def delete_test(test_id: int, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    TestService(db).delete(test_id)


# --- taking a test ---

@router.get("/public", response_model=List[PublicTest])
def public_tests(user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return TestService(db).list_public(user)


@router.post("/public/{test_id}/start", response_model=AttemptOut)
def start_public(test_id: int, user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return TestService(db).start_public(test_id, user)


@router.post("/invite/{token}/start", response_model=AttemptOut)
def start_with_invite(token: str, user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return TestService(db).start_with_invite(token, user)


@router.get("/history", response_model=TestHistory)
def my_history(user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    """Your past test attempts: what you solved and how long each took."""
    return TestService(db).history(user.id)


@router.get("/attempts/active", response_model=List[ActiveAttempt])
def active_attempts(user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return TestService(db).active_attempts(user)


@router.get("/attempts/{attempt_id}", response_model=AttemptOut)
def get_attempt(attempt_id: int, user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return TestService(db).get_attempt(attempt_id, user)


@router.post("/attempts/{attempt_id}/finish", response_model=AttemptOut)
def finish_attempt(attempt_id: int, user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return TestService(db).finish(attempt_id, user)
