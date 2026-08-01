import os
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv
from sqlalchemy.pool import NullPool
import urllib.parse

load_dotenv()

user = os.getenv('DB_USER')
password = urllib.parse.quote_plus(os.getenv('PASSWORD'))
host = os.getenv('DB_HOST')
port = os.getenv('DB_PORT')
db_name = os.getenv('DB_NAME')
db_ssl_mode = os.getenv('DB_SSL_MODE', 'require')

SQLALCHEMY_DATABASE_URL = f"postgresql://{user}:{password}@{host}:{port}/{db_name}?sslmode={db_ssl_mode}"
connect_args = {} if db_ssl_mode == 'disable' else {"sslmode": db_ssl_mode}
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args=connect_args, poolclass=NullPool)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# --- Redis Configuration ---
import redis
_redis_ssl = os.getenv('REDIS_SSL', 'true').lower() == 'true'
_redis_scheme = 'rediss' if _redis_ssl else 'redis'
_redis_password = os.getenv('REDIS_PASSWORD', '')
_redis_auth = f"default:{_redis_password}@" if _redis_password else ""
redis_client = redis.from_url(
    f"{_redis_scheme}://{_redis_auth}{os.getenv('REDIS_HOST')}:{os.getenv('REDIS_PORT')}",
    decode_responses=True
)