"""Live progress of a submission, shared between the engine (which writes it while judging) and this
service (which long-polls it for the browser and API users).

Stored in Redis as JSON under progress:<submissionId>:

    {"v": 7, "stage": "RUNNING", "total": 26, "started": 8, "completed": 7, "ts": 1759766400.2}

`v` only ever increases, so a client can ask "anything newer than the version I have?" and never miss
or repeat an update. This service writes v=1 (QUEUED) when the submission is accepted; the engine
continues from v=2. Stages, in order:

    QUEUED     accepted, waiting for the engine
    PREPARING  the engine picked it up and is loading tests and starting the sandbox
    RUNNING    the sandbox is running; started/completed count the tests (started=0: compiling/starting)
    JUDGING    all tests ran, comparing outputs
    DONE       finished; the verdict is in the database
    ERROR      the engine failed to judge it
"""
import json
import logging
import time
from typing import Optional

from app.database import redis_client

logger = logging.getLogger(__name__)

TTL_SECONDS = 15 * 60
TERMINAL_STAGES = ("DONE", "ERROR")


def key(submission_id: str) -> str:
    return f"progress:{submission_id}"


def write(submission_id: str, record: dict) -> None:
    record = {**record, "ts": time.time()}
    redis_client.setex(key(submission_id), TTL_SECONDS, json.dumps(record))


def write_queued(submission_id: str) -> None:
    """Best effort: a missing progress record only costs the progress bar, never the submission."""
    try:
        write(submission_id, {"v": 1, "stage": "QUEUED", "total": None, "started": 0, "completed": 0})
    except Exception as e:
        logger.warning("could not record queued progress: %s", e)


def read(submission_id: str) -> Optional[dict]:
    raw = redis_client.get(key(submission_id))
    if not raw:
        return None
    try:
        return json.loads(raw)
    except ValueError:
        return None


def percent(stage: str, total: Optional[int], completed: int) -> int:
    """One scale for every client: queueing and setup take the first 30%, the tests 30-90%, checking 90-100%."""
    if stage in TERMINAL_STAGES:
        return 100
    if stage == "QUEUED":
        return 5
    if stage == "PREPARING":
        return 15
    if stage == "RUNNING":
        if not total:
            return 30
        return 30 + int(60 * min(max(completed, 0), total) / total)
    if stage == "JUDGING":
        return 95
    return 0


def message(stage: str, total: Optional[int], started: int, completed: int, language: Optional[str]) -> str:
    if stage == "QUEUED":
        return "Queued"
    if stage == "PREPARING":
        return "Preparing the sandbox"
    if stage == "RUNNING":
        if started == 0:
            return "Compiling" if language == "cpp" else "Starting"
        if total and completed >= total:
            return "Finishing"
        return f"Running test {started} of {total}" if total else f"Running test {started}"
    if stage == "JUDGING":
        return "Checking answers"
    if stage == "ERROR":
        return "Judging failed"
    return "Done"


def describe(raw: dict, language: Optional[str]) -> dict:
    stage = raw.get("stage", "QUEUED")
    total = raw.get("total")
    started = int(raw.get("started") or 0)
    completed = int(raw.get("completed") or 0)
    return {
        "version": int(raw.get("v") or 0),
        "stage": stage,
        "message": message(stage, total, started, completed, language),
        "percent": percent(stage, total, completed),
        "total": total,
        "started": started,
        "completed": completed,
        "terminal": stage in TERMINAL_STAGES,
    }
