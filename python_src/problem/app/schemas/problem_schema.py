from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
from enum import Enum

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

class CodeRequest(BaseModel):
    # This replaces the Code.java DTO
    problemId: int
    code: str
    language: str

class TestDTO(BaseModel):
    userId: Optional[int] = None
    code: str
    language: str
    problemId: int
    input: str
    output: Optional[str] = None
    status: Optional[str] = "IN_PROGRESS"
    submissionId: Optional[str] = None

class SubmissionResponse(BaseModel):
    id: int
    # Alias maps database 'submission_id' to JSON 'submissionId'
    submissionId: str = Field(validation_alias="submission_id")
    userId: int = Field(validation_alias="user_id")
    problemId: int = Field(validation_alias="problem_id")
    code: str
    language: str
    status: SubmissionStatus
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
    testCases: List[TestCaseDTO] = Field(default=[], alias="testCases")

    class Config:
        populate_by_name = True
        from_attributes = True