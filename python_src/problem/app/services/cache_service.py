import json
from app.database import redis_client

class CacheService:
    @staticmethod
    def set_object(key: str, value: any, expire_seconds: int = 600):
        # Python equivalent of objectMapper.writeValueAsString
        json_data = json.dumps(value)
        redis_client.setex(key, expire_seconds, json_data)

    @staticmethod
    def get_object(key: str):
        data = redis_client.get(key)
        return json.loads(data) if data else None

    @staticmethod
    def delete(key: str):
        redis_client.delete(key)
        
    @staticmethod
    def get_value(key: str):
        return redis_client.get(key)

    @staticmethod
    def delete_pattern(pattern: str):
        count = 0
        for key in redis_client.scan_iter(pattern):
            redis_client.delete(key)
            count += 1
        return count