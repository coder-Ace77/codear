from datetime import datetime
from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, Field, field_validator, model_validator

MAX_PROBLEMS_PER_TEST = 50
MAX_DURATION_MINUTES = 12 * 60
MAX_INVITEES = 200


class Visibility(str, Enum):
    PUBLIC = "PUBLIC"      # anyone signed in can find and start it
    PRIVATE = "PRIVATE"    # only the users invited by name, each through their own link


class SelectionMode(str, Enum):
    ALL_PROBLEMS = "ALL_PROBLEMS"  # random problems from the whole problem set
    POOL = "POOL"                  # random problems from the ones the admin picked


class SlotDifficulty(str, Enum):
    ANY = "ANY"
    EASY = "EASY"
    MEDIUM = "MEDIUM"
    HARD = "HARD"


class InviteSettings(BaseModel):
    usernames: List[str] = Field(default_factory=list, max_length=MAX_INVITEES)
    # None: follow the test's multipleAttempts setting
    singleUse: Optional[bool] = None
    # links stop working at this moment, whether or not they were used
    expiresAt: Optional[datetime] = None

    @field_validator("usernames")
    @classmethod
    def clean(cls, names: List[str]) -> List[str]:
        seen, out = set(), []
        for name in names:
            name = name.strip()
            if name and name.lower() not in seen:
                seen.add(name.lower())
                out.append(name)
        return out


class CreateTestRequest(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    description: Optional[str] = Field(None, max_length=5000)
    visibility: Visibility
    selectionMode: SelectionMode
    # required for POOL, ignored otherwise
    poolProblemIds: Optional[List[int]] = None
    problemCount: int = Field(ge=1, le=MAX_PROBLEMS_PER_TEST)
    # Optional: the difficulty wanted for each problem, in order (one entry per problem). Leave out for any.
    slotDifficulties: Optional[List[SlotDifficulty]] = None
    # None = untimed
    durationMinutes: Optional[int] = Field(None, ge=1, le=MAX_DURATION_MINUTES)
    # False: one attempt per user. True: a user may start again after finishing or running out of time
    multipleAttempts: bool = False
    # PRIVATE tests: who gets a link and how it behaves
    invites: InviteSettings = Field(default_factory=InviteSettings)

    @field_validator("title")
    @classmethod
    def title_not_blank(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("title must not be blank")
        return value

    @model_validator(mode="after")
    def consistent(self):
        if self.slotDifficulties is not None:
            if len(self.slotDifficulties) != self.problemCount:
                raise ValueError("slotDifficulties needs exactly one entry per problem")
            if all(d == SlotDifficulty.ANY for d in self.slotDifficulties):
                self.slotDifficulties = None
        if self.selectionMode == SelectionMode.POOL:
            pool = list(dict.fromkeys(self.poolProblemIds or []))
            if not pool:
                raise ValueError("poolProblemIds is required when selectionMode is POOL")
            if self.problemCount > len(pool):
                raise ValueError("problemCount cannot be larger than the number of selected problems")
            self.poolProblemIds = pool
        else:
            self.poolProblemIds = None
        if self.visibility == Visibility.PUBLIC and self.invites.usernames:
            raise ValueError("invites only apply to PRIVATE tests")
        return self


class AddInvitesRequest(InviteSettings):
    usernames: List[str] = Field(min_length=1, max_length=MAX_INVITEES)


class InviteOut(BaseModel):
    id: int
    userId: int
    username: Optional[str] = None
    token: str
    singleUse: bool
    expiresAt: Optional[datetime] = None
    revokedAt: Optional[datetime] = None
    # "/test/<token>"; the frontend prefixes its own origin
    path: str
    used: bool = False


class TestSummary(BaseModel):
    id: int
    title: str
    description: Optional[str] = None
    visibility: Visibility
    selectionMode: SelectionMode
    problemCount: int
    slotDifficulties: Optional[List[SlotDifficulty]] = None
    durationMinutes: Optional[int] = None
    multipleAttempts: bool = False
    isActive: bool
    createdAt: Optional[datetime] = None
    attempts: int = 0
    invites: int = 0


class PublicTest(TestSummary):
    # the caller's standing: NOT_STARTED, IN_PROGRESS (attemptId is the one to resume),
    # DONE (one-attempt test already taken) or RETAKE (taken before, may start again)
    myStatus: str = "NOT_STARTED"
    myAttemptId: Optional[int] = None


class TestDetail(TestSummary):
    poolProblemIds: Optional[List[int]] = None
    inviteList: List[InviteOut] = Field(default_factory=list)


class CreatedTest(BaseModel):
    test: TestDetail
    # usernames that matched nobody, so the admin can fix typos
    unknownUsernames: List[str] = Field(default_factory=list)


class AttemptProblem(BaseModel):
    # no difficulty here on purpose: it is not shown while a test is running
    id: int
    title: str


class AttemptOut(BaseModel):
    attemptId: int
    testId: int
    title: str
    description: Optional[str] = None
    startedAt: datetime
    expiresAt: Optional[datetime] = None
    # seconds left, None when untimed; 0 once time is up
    secondsRemaining: Optional[int] = None
    # seconds since it started, stopping when it ended; lets untimed tests show a clock too
    secondsElapsed: int = 0
    finished: bool
    problems: List[AttemptProblem]
    # ids of the problems already accepted in this attempt
    solved: List[int] = Field(default_factory=list)


class ProblemResult(BaseModel):
    id: int
    title: str
    solved: bool
    submissions: int
    solvedAt: Optional[datetime] = None


class AttemptResult(BaseModel):
    attemptId: int
    userId: int
    username: Optional[str] = None
    startedAt: datetime
    finishedAt: Optional[datetime] = None
    expiresAt: Optional[datetime] = None
    over: bool
    solvedCount: int
    total: int
    problems: List[ProblemResult]


class ActiveAttempt(BaseModel):
    attemptId: int
    testId: int
    title: str
    startedAt: datetime
    expiresAt: Optional[datetime] = None
    secondsRemaining: Optional[int] = None
    solved: int
    total: int
