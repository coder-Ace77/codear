from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text
from sqlalchemy.orm import relationship
from app.database import Base
import datetime

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True)
    name = Column(String)
    email = Column(String, unique=True)
    password = Column(String)
    role = Column(String, default="USER")
    daily_streak = Column(Integer, default=0)
    problem_solved_easy = Column(Integer, default=0)
    problem_solved_medium = Column(Integer, default=0)
    problem_solved_hard = Column(Integer, default=0)
    problem_solved_total = Column(Integer, default=0)
    chat_count_week = Column(Integer, default=0)
    last_chat_reset = Column(DateTime, default=datetime.datetime.utcnow)

class ChatMessage(Base):
    __tablename__ = "chat_messages"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    content = Column(Text)
    role = Column(String)
    problem_id = Column(String, nullable=True) # Assuming string ID for problem, nullable for old messages
    timestamp = Column(DateTime, default=datetime.datetime.utcnow)

class ApiKey(Base):
    """A key a user creates to call the API without signing in. Only a SHA-256 of the key is stored.
    The problem service maps this same table to authenticate requests, so keep both definitions in step."""
    __tablename__ = "api_keys"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, index=True, nullable=False)
    name = Column(String, nullable=False)
    prefix = Column(String, nullable=False)  # first characters of the key, so a person can tell keys apart
    key_hash = Column(String, unique=True, index=True, nullable=False)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    last_used_at = Column(DateTime, nullable=True)
    revoked_at = Column(DateTime, nullable=True)
