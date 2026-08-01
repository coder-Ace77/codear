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