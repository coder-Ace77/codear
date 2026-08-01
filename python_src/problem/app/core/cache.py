import redis
import json
import os

_redis_ssl = os.getenv('REDIS_SSL', 'true').lower() == 'true'
_redis_scheme = 'rediss' if _redis_ssl else 'redis'
_redis_password = os.getenv('REDIS_PASSWORD', '')
_redis_auth = f":{_redis_password}@" if _redis_password else ""
r = redis.from_url(
    f"{_redis_scheme}://{_redis_auth}{os.getenv('REDIS_HOST')}:{os.getenv('REDIS_PORT')}",
    decode_responses=True
)

def set_cache(key, value, expiry=600):
    if isinstance(value, (dict, list)):
        value = json.dumps(value)
    r.setex(key, expiry, str(value))

def get_cache(key, is_json=False):
    data = r.get(key)
    if data and is_json:
        return json.loads(data)
    return data

def delete_cache(key):
    r.delete(key)