from pydantic import BaseModel, Field, field_validator
from typing import List, Optional
from datetime import datetime
from enum import Enum

from app.core.checker_modes import CHECKER_MODES

# --- Enums ---
class SubmissionStatus(str, Enum):
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    PASSED = "PASSED"

# --- DTOs ---

class ProblemSummaryDTO(BaseModel):
    id: int
    title: str
    tags: List[str]
    difficulty: str

    class Config:
        from_attributes = True

class TestCaseDTO(BaseModel):
    id: Optional[int] = None
    input: str
    output: str
    isSample: bool = Field(alias="isSample")

    class Config:
        populate_by_name = True
        from_attributes = True

class ProblemSendDTO(BaseModel):
    id: int
    title: str
    description: str
    # Change these to Optional[str]
    inputDescription: Optional[str] = None 
    outputDescription: Optional[str] = None
    constraints: Optional[str] = None
    difficulty: str
    tags: List[str]
    timeLimitMs: int
    memoryLimitMb: int
    testCases: List[TestCaseDTO] = Field(default=[], alias="testCases")

    class Config:
        from_attributes = True
        populate_by_name = True

class ProblemsMetaData(BaseModel):
    count: int
    tags: List[str]

def _not_blank(value: str) -> str:
    if not value.strip():
        raise ValueError("Code must not be empty")
    return value


class CodeRequest(BaseModel):
    # This replaces the Code.java DTO
    problemId: int
    code: str = Field(max_length=65536)
    language: str = Field(max_length=16)

    _code_not_blank = field_validator("code")(_not_blank)

class TestDTO(BaseModel):
    userId: Optional[int] = None
    code: str = Field(max_length=65536)
    language: str = Field(max_length=16)
    problemId: int
    input: str = Field(max_length=65536)
    output: Optional[str] = None
    status: Optional[str] = "IN_PROGRESS"
    submissionId: Optional[str] = None

    _code_not_blank = field_validator("code")(_not_blank)

class SubmissionResponse(BaseModel):
    id: int
    # Alias maps database 'submission_id' to JSON 'submissionId'
    submissionId: str = Field(validation_alias="submission_id")
    userId: int = Field(validation_alias="user_id")
    problemId: int = Field(validation_alias="problem_id")
    code: str
    language: str
    status: SubmissionStatus
    # Why it got that status: ACCEPTED, WRONG_ANSWER, COMPILE_ERROR, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED,
    # MEMORY_LIMIT_EXCEEDED, OUTPUT_LIMIT_EXCEEDED or SYSTEM_ERROR. Null for submissions judged before verdicts existed.
    verdict: Optional[str] = None
    # 1-based number of the first test that failed, when there was one.
    failedTest: Optional[int] = Field(None, validation_alias="failed_test")
    result: Optional[str] = None
    totalTests: Optional[int] = Field(None, validation_alias="total_tests")
    passedTests: Optional[int] = Field(None, validation_alias="passed_tests")
    submittedAt: datetime = Field(validation_alias="submitted_at")
    timeTakenMs: Optional[int] = Field(None, validation_alias="time_taken_ms")
    memoryUsed: Optional[str] = Field(None, validation_alias="memory_used")

    class Config:
        from_attributes = True
        # This allows camelCase in JSON while keeping snake_case in Python
        populate_by_name = True

from pydantic import BaseModel, Field
from typing import List, Optional

class ProblemDTO(BaseModel):
    id: Optional[int] = None
    title: str
    description: str
    inputDescription: Optional[str] = Field(None, alias="inputDescription")
    outputDescription: Optional[str] = Field(None, alias="outputDescription")
    constraints: Optional[str] = None
    difficulty: str
    tags: List[str] = []
    # Defaults: 1000ms (1s) and 512MB
    timeLimitMs: int = Field(default=1000, alias="timeLimitMs") 
    memoryLimitMb: int = Field(default=512, alias="memoryLimitMb")
    # How answers are compared (see core.checker_modes). Unset means the default: word by word, case-sensitive.
    checker: Optional[str] = None
    # For the FLOAT checker: how far apart two numbers may be.
    checkerTolerance: Optional[float] = Field(None, gt=0)
    testCases: List[TestCaseDTO] = Field(default=[], alias="testCases")

    @field_validator("checker")
    @classmethod
    def known_checker(cls, value):
        if value is None or value == "":
            return None
        value = value.strip().upper()
        if value not in CHECKER_MODES:
            raise ValueError(f"checker must be one of {', '.join(CHECKER_MODES)}")
        return value

    class Config:
        populate_by_name = True
        from_attributes = True