from typing import List

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.auth import CurrentUser, get_current_user, require_admin
from app.database import get_db
from app.schemas.assessment_schema import (
    ActiveAttempt,
    AddInvitesRequest,
    AttemptOut,
    AttemptResult,
    CreatedAssessment,
    CreateAssessmentRequest,
    PublicAssessment,
    AssessmentHistory,
    AssessmentDetail,
    AssessmentSummary,
)
from app.services.assessment_service import AssessmentService

admin_router = APIRouter(prefix="/api/v1/problem/admin/assessments")
router = APIRouter(prefix="/api/v1/problem/assessments")


class ActiveRequest(BaseModel):
    isActive: bool


# --- admin: build and manage assessments ---

@admin_router.post("", response_model=CreatedAssessment, status_code=201)
def create_assessment(req: CreateAssessmentRequest, admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return AssessmentService(db).create(req, admin)


@admin_router.get("", response_model=List[AssessmentSummary])
def list_assessments(_admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return AssessmentService(db).list_assessments()


@admin_router.get("/history", response_model=AssessmentHistory)
def user_history(username: str, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    """Any user's past assessment attempts, looked up by username."""
    return AssessmentService(db).history_by_username(username)


@admin_router.get("/{assessment_id}", response_model=AssessmentDetail)
def get_assessment(assessment_id: int, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return AssessmentService(db).get_detail(assessment_id)


@admin_router.patch("/{assessment_id}", response_model=AssessmentDetail)
def set_active(assessment_id: int, body: ActiveRequest, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return AssessmentService(db).set_active(assessment_id, body.isActive)


@admin_router.post("/{assessment_id}/invites", response_model=CreatedAssessment, status_code=201)
def add_invites(assessment_id: int, body: AddInvitesRequest, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return AssessmentService(db).add_invites(assessment_id, body)


@admin_router.delete("/{assessment_id}/invites/{invite_id}", status_code=204)
def revoke_invite(assessment_id: int, invite_id: int, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    AssessmentService(db).revoke_invite(assessment_id, invite_id)


@admin_router.get("/{assessment_id}/results", response_model=List[AttemptResult])
def results(assessment_id: int, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    return AssessmentService(db).results(assessment_id)


@admin_router.delete("/{assessment_id}", status_code=204)
def delete_assessment(assessment_id: int, _admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    AssessmentService(db).delete(assessment_id)


# --- taking an assessment ---

@router.get("/public", response_model=List[PublicAssessment])
def public_assessments(user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return AssessmentService(db).list_public(user)


@router.post("/public/{assessment_id}/start", response_model=AttemptOut)
def start_public(assessment_id: int, user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return AssessmentService(db).start_public(assessment_id, user)


@router.post("/invite/{token}/start", response_model=AttemptOut)
def start_with_invite(token: str, user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return AssessmentService(db).start_with_invite(token, user)


@router.get("/history", response_model=AssessmentHistory)
def my_history(user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    """Your past assessment attempts: what you solved and how long each took."""
    return AssessmentService(db).history(user.id)


@router.get("/attempts/active", response_model=List[ActiveAttempt])
def active_attempts(user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return AssessmentService(db).active_attempts(user)


@router.get("/attempts/{attempt_id}", response_model=AttemptOut)
def get_attempt(attempt_id: int, user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return AssessmentService(db).get_attempt(attempt_id, user)


@router.post("/attempts/{attempt_id}/finish", response_model=AttemptOut)
def finish_attempt(attempt_id: int, user: CurrentUser = Depends(get_current_user), db: Session = Depends(get_db)):
    return AssessmentService(db).finish(attempt_id, user)
