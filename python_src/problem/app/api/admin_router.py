from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.auth import CurrentUser, require_admin
from app.database import get_db
from app.services.admin_stats_service import AdminStatsService

router = APIRouter(prefix="/api/v1/problem/admin")


@router.get("/stats")
def stats(_admin: CurrentUser = Depends(require_admin), db: Session = Depends(get_db)):
    """Queue depth, submission volume and rate-limit activity, for the admin dashboard."""
    return AdminStatsService(db).stats()


@router.get("/submissions")
def recent_submissions(
    limit: int = Query(50, ge=1, le=200),
    _admin: CurrentUser = Depends(require_admin),
    db: Session = Depends(get_db),
):
    return AdminStatsService(db).recent_submissions(limit)
