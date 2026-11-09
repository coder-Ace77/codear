import os

os.environ.setdefault("JWT_SECRET", "test-secret")
os.environ.setdefault("SQS_ACCESS_KEY", "test")
os.environ.setdefault("SQS_SECRET_KEY", "test")
os.environ.setdefault("REDIS_HOST", "localhost")
os.environ.setdefault("REDIS_PORT", "6379")
os.environ.setdefault("REDIS_SSL", "false")
