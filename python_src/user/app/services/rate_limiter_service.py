from datetime import datetime, timedelta
from typing import Tuple, Optional
import math

class RateLimiterService:
    @staticmethod
    def check_rate_limit(
        current_count: int, 
        last_reset: Optional[datetime], 
        max_requests: int, 
        period_seconds: int
    ) -> Tuple[bool, Optional[datetime], Optional[tuple]]:
        """
        Checks if the request is allowed based on Token Bucket algorithm.
        
        Args:
            current_count: Current debt/tokens used.
            last_reset: Last time usage was updated.
            max_requests: Bucket capacity.
            period_seconds: Period for the rate (e.g. 1 week in seconds).

        Returns:
            Tuple containing:
            - is_allowed (bool)
            - next_available_time (datetime | None): If blocked, when it will be allowed.
            - update_values (tuple | None): (new_count, new_reset_time) to save to DB if allowed.
        """
        now = datetime.utcnow()
        
        # Calculate refill rate (tokens per second)
        refill_rate = max_requests / period_seconds
        
        # Calculate tokens to restore (debt to reduce)
        last_reset_time = last_reset or now
        time_passed = (now - last_reset_time).total_seconds()
        debt_reduction = time_passed * refill_rate
        
        # Update current debt
        current_debt = max(0.0, float(current_count) - debt_reduction)
        
        
        # Case 1: Request Allowed
        if current_debt + 1 <= max_requests:
            new_count = math.ceil(current_debt + 1)
            return True, None, (new_count, now)
            
        # Case 2: Request Blocked
        # Calculate when debt drops enough to allow 1 request
        # Target debt = max_requests - 1
        debt_to_shed = current_debt - (max_requests - 1)
        seconds_to_wait = debt_to_shed / refill_rate
        next_available_time = now + timedelta(seconds=seconds_to_wait)
        
        return False, next_available_time, None
