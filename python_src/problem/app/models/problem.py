from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text, BigInteger, Boolean, Float, Enum as SQLEnum, Index
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

class User(Base):
    """The user service owns this table. The problem service only reads it, to confirm a token's
    user still exists and to read their current role, so only those columns are mapped."""
    __tablename__ = "users"
    id = Column(Integer, primary_key=True)
    username = Column(String)
    role = Column(String)

class ApiKey(Base):
    """Owned by the user service, which creates and revokes keys. Mapped here only to authenticate
    requests, so keep this definition identical to the user service's: whichever service starts first
    creates the table."""
    __tablename__ = "api_keys"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, index=True, nullable=False)
    name = Column(String, nullable=False)
    prefix = Column(String, nullable=False)
    key_hash = Column(String, unique=True, index=True, nullable=False)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    last_used_at = Column(DateTime, nullable=True)
    revoked_at = Column(DateTime, nullable=True)

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
    # how answers are compared (a core.checker_modes name); NULL means the default
    checker = Column(String(40), nullable=True)
    checker_tolerance = Column(Float, nullable=True)

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
    # why it got that status (an engine Verdict name) and the first failing test; NULL for old submissions
    verdict = Column(String(40), nullable=True)
    failed_test = Column(Integer, nullable=True)
    # the test attempt this was submitted for; NULL for ordinary practice submissions
    attempt_id = Column(Integer, nullable=True, index=True)
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

class CustomTest(Base):
    """A test an admin put together: a set of problems drawn at random when someone starts it,
    optionally timed, either open to everyone or reached only through per-user invite links."""
    __tablename__ = "custom_tests"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(200), nullable=False)
    description = Column(Text)
    visibility = Column(String(10), nullable=False)       # PUBLIC | PRIVATE
    # ALL_PROBLEMS draws from every problem; POOL draws from `pool_problem_ids` only
    selection_mode = Column(String(20), nullable=False)
    pool_problem_ids = Column(ARRAY(BigInteger), nullable=True)
    problem_count = Column(Integer, nullable=False)
    # one entry per problem slot: ANY, EASY, MEDIUM or HARD. NULL = every slot is ANY
    slot_difficulties = Column(ARRAY(String), nullable=True)
    duration_minutes = Column(Integer, nullable=True)     # NULL = untimed
    is_active = Column(Boolean, nullable=False, default=True)
    # False: each user gets one attempt. True: a user may start again once an attempt is over
    allow_retakes = Column(Boolean, nullable=False, default=False)
    created_by = Column(BigInteger, nullable=False)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    invites = relationship("TestInvite", back_populates="test", cascade="all, delete-orphan")
    attempts = relationship("TestAttempt", back_populates="test", cascade="all, delete-orphan")


class TestInvite(Base):
    """A link for one user. The token in the URL is the only secret; the signed-in user must also be
    the invited one. A single-use invite stops working once its attempt is finished or has run out of time."""
    __tablename__ = "test_invites"

    id = Column(Integer, primary_key=True, index=True)
    test_id = Column(Integer, ForeignKey("custom_tests.id"), nullable=False, index=True)
    user_id = Column(BigInteger, nullable=False)
    token = Column(String(64), unique=True, index=True, nullable=False)
    single_use = Column(Boolean, nullable=False, default=True)
    expires_at = Column(DateTime, nullable=True)
    revoked_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    test = relationship("CustomTest", back_populates="invites")


class TestAttempt(Base):
    """One user taking one test. The problems are drawn when it starts and fixed from then on."""
    __tablename__ = "test_attempts"

    id = Column(Integer, primary_key=True, index=True)
    test_id = Column(Integer, ForeignKey("custom_tests.id"), nullable=False, index=True)
    invite_id = Column(Integer, ForeignKey("test_invites.id"), nullable=True, index=True)
    user_id = Column(BigInteger, nullable=False, index=True)
    problem_ids = Column(ARRAY(BigInteger), nullable=False)
    # titles as they were when drawn, so the history still reads right if a problem is renamed or deleted
    problem_titles = Column(ARRAY(String), nullable=True)
    started_at = Column(DateTime, nullable=False, default=datetime.datetime.utcnow)
    expires_at = Column(DateTime, nullable=True)
    finished_at = Column(DateTime, nullable=True)

    test = relationship("CustomTest", back_populates="attempts")
