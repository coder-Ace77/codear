from datetime import datetime, timedelta

from app.services.rate_limiter_service import RateLimiterService

WEEK = 7 * 24 * 3600
MAX_REQUESTS = 20


def test_first_request_is_allowed():
    now = datetime.utcnow()

    is_allowed, next_time, update_vals = RateLimiterService.check_rate_limit(
        current_count=0, last_reset=now, max_requests=MAX_REQUESTS, period_seconds=WEEK
    )

    assert is_allowed
    assert next_time is None
    assert update_vals[0] == 1


def test_request_at_capacity_is_blocked():
    now = datetime.utcnow()

    is_allowed, next_time, update_vals = RateLimiterService.check_rate_limit(
        current_count=MAX_REQUESTS,
        last_reset=now,
        max_requests=MAX_REQUESTS,
        period_seconds=WEEK,
    )

    assert not is_allowed
    assert update_vals is None
    assert next_time > now


def test_last_request_within_capacity_is_allowed():
    now = datetime.utcnow()

    is_allowed, _, update_vals = RateLimiterService.check_rate_limit(
        current_count=MAX_REQUESTS - 1,
        last_reset=now,
        max_requests=MAX_REQUESTS,
        period_seconds=WEEK,
    )

    assert is_allowed
    assert update_vals[0] == MAX_REQUESTS


def test_bucket_refills_over_time():
    """A full bucket left untouched for a whole period is empty again."""
    long_ago = datetime.utcnow() - timedelta(seconds=WEEK)

    is_allowed, _, update_vals = RateLimiterService.check_rate_limit(
        current_count=MAX_REQUESTS,
        last_reset=long_ago,
        max_requests=MAX_REQUESTS,
        period_seconds=WEEK,
    )

    assert is_allowed
    assert update_vals[0] == 1


def test_partial_refill_shortens_the_wait():
    """Half a period of refill leaves the bucket half full, not empty."""
    half_period_ago = datetime.utcnow() - timedelta(seconds=WEEK // 2)

    is_allowed, _, update_vals = RateLimiterService.check_rate_limit(
        current_count=MAX_REQUESTS,
        last_reset=half_period_ago,
        max_requests=MAX_REQUESTS,
        period_seconds=WEEK,
    )

    assert is_allowed
    assert update_vals[0] == MAX_REQUESTS // 2 + 1


def test_missing_last_reset_is_treated_as_now():
    is_allowed, next_time, _ = RateLimiterService.check_rate_limit(
        current_count=MAX_REQUESTS,
        last_reset=None,
        max_requests=MAX_REQUESTS,
        period_seconds=WEEK,
    )

    assert not is_allowed
    assert next_time is not None
