from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text, BigInteger, Boolean, Enum as SQLEnum, Index
from sqlalchemy.dialects.postgresql import ARRAY
from sqlalchemy.orm import relationship
from app.database import Base
import datetime
import enum

class SubmissionStatus(str, enum.Enum):
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    PASSED = "PASSED"

class Problem(Base):
    __tablename__ = "problems"
    __table_args__ = (
        Index("idx_problem_title", "title"),
        Index("idx_problem_difficulty", "difficulty"),
    )

    id = Column(BigInteger, primary_key=True, index=True)
    title = Column(String)
    description = Column(Text) # columnDefinition = "TEXT"
    input_description = Column(String)
    output_description = Column(String)
    constraints = Column(String)
    difficulty = Column(String)
    tags = Column(ARRAY(String)) # columnDefinition = "text[]"
    time_limit_ms = Column(BigInteger)
    memory_limit_mb = Column(Integer)

    test_cases = relationship("TestCase", back_populates="problem", cascade="all, delete-orphan")
class TestCase(Base):
    __tablename__ = "test_cases"
    id = Column(Integer, primary_key=True, index=True)
    input = Column(Text)
    output = Column(Text)
    is_sample = Column(Boolean, default=False)
    problem_id = Column(Integer, ForeignKey("problems.id"))

    problem = relationship("Problem", back_populates="test_cases")

class Submission(Base):
    __tablename__ = "submissions"
    id = Column(Integer, primary_key=True, index=True)
    submission_id = Column(String, unique=True, index=True)
    user_id = Column(BigInteger)
    problem_id = Column(BigInteger)
    code = Column(Text)
    language = Column(String)
    status = Column(SQLEnum(SubmissionStatus), default=SubmissionStatus.IN_PROGRESS)
    result = Column(Text)
    total_tests = Column(Integer)
    passed_tests = Column(Integer)
    submitted_at = Column(DateTime, default=datetime.datetime.utcnow)
    time_taken_ms = Column(BigInteger)
    memory_used = Column(String)

class Editorial(Base):
    __tablename__ = "editorials"
    id = Column(BigInteger, primary_key=True, index=True)
    problem_id = Column(BigInteger, ForeignKey("problems.id"))
    user_id = Column(BigInteger) # We don't have user table here, so just ID
    username = Column(String)
    title = Column(String)
    content = Column(Text)
    is_admin = Column(Boolean, default=False)
    upvotes = Column(Integer, default=0)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    updated_at = Column(DateTime, onupdate=datetime.datetime.utcnow)