"""A validated problem search: what the caller asked for, in the form the query builder uses."""
from dataclasses import dataclass, field
from typing import List, Optional

from fastapi import HTTPException, Query

from app.core.search_options import (
    ALL_DIFFICULTIES,
    DEFAULT_PAGE_SIZE,
    DEFAULT_SORT,
    DIFFICULTIES,
    MAX_PAGE_SIZE,
    MAX_TAGS,
    SEARCH_MAX_LENGTH,
    SORT_ALIASES,
    SORT_OPTIONS,
    TAG_MAX_LENGTH,
    TAG_MODE_ANY,
    TAG_MODES,
)


@dataclass(frozen=True)
class SearchQuery:
    text: str = ""
    difficulty: Optional[str] = None  # lower case, or None for "any"
    tags: List[str] = field(default_factory=list)  # lower case, no duplicates
    tag_mode: str = TAG_MODE_ANY
    sort: str = DEFAULT_SORT
    page: int = 0
    size: int = DEFAULT_PAGE_SIZE

    @property
    def offset(self) -> int:
        return self.page * self.size


def _reject(message: str):
    raise HTTPException(status_code=422, detail=message)


def parse_search_query(
    search: Optional[str] = Query(None, max_length=SEARCH_MAX_LENGTH, description="words from the title, statement or tags"),
    difficulty: Optional[str] = Query(None, description="easy, medium, hard, or all"),
    tags: Optional[List[str]] = Query(None, description="repeat for several tags"),
    tagMode: str = Query(TAG_MODE_ANY, description="any: at least one of the tags; all: every tag"),
    sortBy: str = Query(DEFAULT_SORT, description=", ".join(SORT_OPTIONS)),
    page: int = Query(0, ge=0),
    size: int = Query(DEFAULT_PAGE_SIZE, ge=1, le=MAX_PAGE_SIZE),
) -> SearchQuery:
    """FastAPI dependency: turns the raw query string into a SearchQuery, or a 422 that says what is wrong."""
    sort = SORT_ALIASES.get(sortBy.strip().lower(), sortBy.strip().lower())
    if sort not in SORT_OPTIONS:
        _reject(f"sortBy must be one of {', '.join(SORT_OPTIONS)}")

    wanted_difficulty = (difficulty or "").strip().lower()
    if wanted_difficulty in ("", ALL_DIFFICULTIES):
        wanted_difficulty = None
    elif wanted_difficulty not in DIFFICULTIES:
        _reject(f"difficulty must be one of {', '.join(DIFFICULTIES)}, or {ALL_DIFFICULTIES}")

    mode = tagMode.strip().lower()
    if mode not in TAG_MODES:
        _reject(f"tagMode must be one of {', '.join(TAG_MODES)}")

    cleaned_tags: List[str] = []
    for tag in tags or []:
        tag = tag.strip().lower()
        if not tag:
            continue
        if len(tag) > TAG_MAX_LENGTH:
            _reject(f"a tag may have at most {TAG_MAX_LENGTH} characters")
        if tag not in cleaned_tags:
            cleaned_tags.append(tag)
    if len(cleaned_tags) > MAX_TAGS:
        _reject(f"at most {MAX_TAGS} tags may be combined")

    return SearchQuery(
        text=(search or "").strip(),
        difficulty=wanted_difficulty,
        tags=cleaned_tags,
        tag_mode=mode,
        sort=sort,
        page=page,
        size=size,
    )
