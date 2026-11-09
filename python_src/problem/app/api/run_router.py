import asyncio
import uuid

from fastapi import APIRouter, Depends, HTTPException

from app.core.auth import CurrentUser, get_current_user
from app.core.sqs import RUN_QUEUE_URL, send_to_queue
from app.schemas.problem_schema import RunRequest
from app.services.cache_service import CacheService

router = APIRouter(prefix="/api/v1/problem")

RUN_NOT_FOUND = "Run not found. It may have expired or never existed."


@router.post("/run")
async def run_code(run: RunRequest, user: CurrentUser = Depends(get_current_user)):
    run_id = str(uuid.uuid4())
    run.runId = run_id
    run.status = "IN_PROGRESS"
    run.userId = user.id
    send_to_queue(run.model_dump(), queue_url=RUN_QUEUE_URL)
    CacheService.set_object(run_id, run.model_dump(), expire_seconds=600)
    return {"message": "Run in queue", "runId": run_id}


@router.get("/runs/{runId}")
async def poll_run(runId: str, user: CurrentUser = Depends(get_current_user)):
    max_wait_time = 1
    poll_interval = 1
    waited = 0

    def visible_to_caller(result) -> bool:
        return bool(result) and (result.get("userId") == user.id or user.is_admin)

    run = CacheService.get_object(runId)
    if not visible_to_caller(run):
        raise HTTPException(status_code=404, detail=RUN_NOT_FOUND)

    while run.get("status") == "IN_PROGRESS" and waited < max_wait_time:
        await asyncio.sleep(poll_interval)
        waited += poll_interval
        run = CacheService.get_object(runId)
        if not visible_to_caller(run):
            break

    if not visible_to_caller(run):
        raise HTTPException(status_code=404, detail=RUN_NOT_FOUND)
    return run
