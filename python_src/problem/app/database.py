import os
import urllib.parse
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import redis
from dotenv import load_dotenv


load_dotenv()

db_ssl_mode = os.getenv('DB_SSL_MODE', 'require')

# DATABASE_URL (e.g. a Neon connection string) takes precedence over the individual DB_* settings.
SQLALCHEMY_DATABASE_URL = os.getenv('DATABASE_URL')
if SQLALCHEMY_DATABASE_URL:
    connect_args = {}
else:
    db_user = os.getenv('DB_USER')
    db_password = os.getenv('PASSWORD')
    db_host = os.getenv('DB_HOST')
    db_port = os.getenv('DB_PORT')
    db_name = os.getenv('DB_NAME')
    encoded_password = urllib.parse.quote_plus(db_password)
    SQLALCHEMY_DATABASE_URL = f"postgresql://{db_user}:{encoded_password}@{db_host}:{db_port}/{db_name}?sslmode={db_ssl_mode}"
    connect_args = {} if db_ssl_mode == 'disable' else {"sslmode": db_ssl_mode}

# Keep a small pool so warm requests reuse their connection instead of paying a fresh TLS handshake each time.
# pre_ping replaces connections the server dropped while idle (or while a Lambda container was frozen).
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args=connect_args,
    pool_size=int(os.getenv('DB_POOL_SIZE', '5')),
    max_overflow=int(os.getenv('DB_MAX_OVERFLOW', '5')),
    pool_pre_ping=True,
    pool_recycle=300,
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# --- Redis Configuration ---
_redis_ssl = os.getenv('REDIS_SSL', 'true').lower() == 'true'
_redis_scheme = 'rediss' if _redis_ssl else 'redis'
_redis_password = os.getenv('REDIS_PASSWORD', '')
_redis_auth = f"default:{_redis_password}@" if _redis_password else ""
redis_client = redis.from_url(
    f"{_redis_scheme}://{_redis_auth}{os.getenv('REDIS_HOST')}:{os.getenv('REDIS_PORT')}",
    decode_responses=True
)