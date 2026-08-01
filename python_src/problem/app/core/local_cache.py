import time
from threading import Lock
from typing import Any, Optional, Dict, Tuple

class LocalCache:
    """
    A simple thread-safe in-memory cache with TTL (Time To Live).
    """
    _storage: Dict[str, Tuple[Any, float]] = {}
    _lock = Lock()
    
    # Optional: Max size to prevent memory leaks if many unique keys are generated
    MAX_SIZE = 1000 

    @classmethod
    def get(cls, key: str) -> Optional[Any]:
        with cls._lock:
            if key in cls._storage:
                data, expiry = cls._storage[key]
                if time.time() < expiry:
                    return data
                else:
                    # Lazy expiration
                    del cls._storage[key]
        return None

    @classmethod
    def set(cls, key: str, value: Any, ttl: Optional[float] = 60):
        with cls._lock:
             # Basic eviction if full (remove oldest) - simplistic approach
            if len(cls._storage) >= cls.MAX_SIZE:
                # Remove expired keys first
                now = time.time()
                keys_to_remove = [k for k, v in cls._storage.items() if v[1] != float('inf') and v[1] < now]
                for k in keys_to_remove:
                    del cls._storage[k]
                
                # If still full, remove arbitrary (first in iteration)
                if len(cls._storage) >= cls.MAX_SIZE:
                    first_key = next(iter(cls._storage))
                    del cls._storage[first_key]

            expiry = float('inf') if ttl is None else time.time() + ttl
            cls._storage[key] = (value, expiry)

    @classmethod
    def delete(cls, key: str):
        with cls._lock:
            if key in cls._storage:
                del cls._storage[key]

    @classmethod
    def clear(cls):
        with cls._lock:
            cls._storage.clear()
            
    @classmethod
    def invalidate_prefix(cls, prefix: str):
        with cls._lock:
            keys_to_remove = [k for k in cls._storage if k.startswith(prefix)]
            for k in keys_to_remove:
                del cls._storage[k]
