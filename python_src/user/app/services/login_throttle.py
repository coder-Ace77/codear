import logging

from app.database import redis_client

logger = logging.getLogger(__name__)

MAX_FAILURES = 10
WINDOW_SECONDS = 15 * 60


def _key(email: str) -> str:
    return f"login:fail:{email}"


class LoginThrottle:
    """Counts failed logins per email in Redis and blocks further attempts for the rest of the window.
    If Redis is unreachable it fails open: a cache outage must not lock everyone out."""

    @staticmethod
    def is_blocked(email: str) -> bool:
        try:
            count = redis_client.get(_key(email))
            return count is not None and int(count) >= MAX_FAILURES
        except Exception as e:
            logger.warning("login throttle unavailable: %s", e)
            return False

    @staticmethod
    def record_failure(email: str) -> None:
        try:
            key = _key(email)
            if redis_client.incr(key) == 1:
                redis_client.expire(key, WINDOW_SECONDS)
        except Exception as e:
            logger.warning("login throttle unavailable: %s", e)

    @staticmethod
    def clear(email: str) -> None:
        try:
            redis_client.delete(_key(email))
        except Exception as e:
            logger.warning("login throttle unavailable: %s", e)
