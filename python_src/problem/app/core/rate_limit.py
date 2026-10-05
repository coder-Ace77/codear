import logging
import time

from fastapi import Depends, HTTPException

from app.core.auth import CurrentUser, get_current_user
from app.database import redis_client

logger = logging.getLogger(__name__)

USER_SUBMIT_WINDOW_SECONDS = 10  # one submission per user per 10 seconds
ADMIN_SUBMITS_PER_SECOND = 100

HITS_KEY = "rl:hits:{minute}"


def _now() -> float:
    return time.time()


def _record_hit() -> None:
    """Counts blocked requests per minute, for the admin dashboard."""
    try:
        key = HITS_KEY.format(minute=int(_now() // 60))
        if redis_client.incr(key) == 1:
            redis_client.expire(key, 3600)
    except Exception:
        pass


def _reject(retry_after: int, message: str) -> None:
    _record_hit()
    raise HTTPException(
        status_code=429,
        detail=f"{message} Try again in {retry_after}s.",
        headers={"Retry-After": str(retry_after)},
    )


def enforce_submit_limit(user: CurrentUser) -> None:
    """Users: one submission per 10 seconds. Admins: 100 per second. The same limits apply whether
    the caller used a session or an API key. If Redis is unreachable this fails open, because a
    cache outage should not stop everyone submitting (the submit itself needs Redis anyway)."""
    try:
        if user.is_admin:
            bucket = f"rl:submit:admin:{user.id}:{int(_now())}"
            count = redis_client.incr(bucket)
            if count == 1:
                redis_client.expire(bucket, 2)
            if count > ADMIN_SUBMITS_PER_SECOND:
                _reject(1, f"Admin limit is {ADMIN_SUBMITS_PER_SECOND} submissions per second.")
            return

        key = f"rl:submit:{user.id}"
        if not redis_client.set(key, 1, nx=True, ex=USER_SUBMIT_WINDOW_SECONDS):
            wait = max(int(redis_client.ttl(key)), 1)
            _reject(wait, f"You can submit once every {USER_SUBMIT_WINDOW_SECONDS} seconds.")
    except HTTPException:
        raise
    except Exception as e:
        logger.warning("submit rate limiter unavailable: %s", e)


def rate_limited_submitter(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    enforce_submit_limit(user)
    return user
