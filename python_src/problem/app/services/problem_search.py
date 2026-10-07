"""Finding problems: filters, free-text search and sorting, in one query.

The SQL is assembled from fixed fragments. Anything a caller chooses (a sort order, a difficulty, tags, words) is either
looked up in a table of allowed fragments or passed as a bound parameter, never pasted into the text.
"""
from dataclasses import dataclass
from typing import Any, Dict, List, Optional, Tuple

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.core import search_options as options
from app.schemas.search_schema import SearchQuery

# --- reusable SQL pieces ---

_TAGS = "COALESCE(p.tags, '{}')"
_SUBMISSIONS = "COALESCE(s.submissions, 0)"
_SOLVERS = "COALESCE(s.solvers, 0)"
_ACCEPTANCE = "(CASE WHEN COALESCE(s.submissions, 0) = 0 THEN NULL ELSE s.accepted::float / s.submissions END)"
_DIFFICULTY_RANK = ("(CASE LOWER(p.difficulty) WHEN 'easy' THEN 1 WHEN 'medium' THEN 2 WHEN 'hard' THEN 3 ELSE 4 END)")

# A problem matches a search word when the word is inside its title, statement or one of its tags, or when the
# statement's stemmed words match ("balance" finds "balanced"). LIKE wildcards in the word are escaped first.
_TEXT_MATCH = f"""(
    p.title ILIKE :like ESCAPE '\\'
    OR p.description ILIKE :like ESCAPE '\\'
    OR EXISTS (SELECT 1 FROM unnest({_TAGS}) AS t WHERE t ILIKE :like ESCAPE '\\')
    OR to_tsvector('english', COALESCE(p.title, '') || ' ' || COALESCE(p.description, '')) @@ plainto_tsquery('english', :search)
)"""

# 0 = the title starts with the words, 1 = the title contains them, 2 = a tag does, 3 = only the statement does
_RELEVANCE = f"""(CASE
    WHEN p.title ILIKE :prefix ESCAPE '\\' THEN 0
    WHEN p.title ILIKE :like ESCAPE '\\' THEN 1
    WHEN EXISTS (SELECT 1 FROM unnest({_TAGS}) AS t WHERE t ILIKE :like ESCAPE '\\') THEN 2
    ELSE 3 END)"""

_STATS = """
    SELECT problem_id,
           COUNT(*) AS submissions,
           COUNT(*) FILTER (WHERE status = 'PASSED') AS accepted,
           COUNT(DISTINCT user_id) FILTER (WHERE status = 'PASSED') AS solvers
    FROM submissions
    GROUP BY problem_id
"""

_ORDER_BY = {
    options.SORT_POPULARITY: f"{_SOLVERS} DESC, {_SUBMISSIONS} DESC, p.id",
    options.SORT_ACCEPTANCE: f"{_ACCEPTANCE} DESC NULLS LAST, {_SUBMISSIONS} DESC, p.id",
    options.SORT_NEWEST: "p.id DESC",
    options.SORT_OLDEST: "p.id",
    options.SORT_TITLE: "LOWER(p.title), p.id",
    options.SORT_EASIEST: f"{_DIFFICULTY_RANK}, p.id",
    options.SORT_HARDEST: f"{_DIFFICULTY_RANK} DESC, p.id",
}


def escape_like(value: str) -> str:
    """Makes `%`, `_` and `\\` in what a person typed match themselves instead of acting as wildcards."""
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


@dataclass(frozen=True)
class SearchPage:
    problems: List[Dict[str, Any]]
    total: int


class ProblemSearch:
    def __init__(self, db: Session):
        self.db = db

    def search(self, query: SearchQuery) -> SearchPage:
        where, params = self._filters(query)
        sql = f"""
            WITH stats AS ({_STATS})
            SELECT p.id, p.title, p.tags, p.difficulty,
                   {_SUBMISSIONS} AS submissions, COALESCE(s.accepted, 0) AS accepted, {_SOLVERS} AS solvers,
                   COUNT(*) OVER () AS total
            FROM problems p
            LEFT JOIN stats s ON s.problem_id = p.id
            {where}
            ORDER BY {self._order_by(query)}
            LIMIT :limit OFFSET :offset
        """
        rows = self.db.execute(text(sql), {**params, "limit": query.size, "offset": query.offset}).fetchall()
        if rows:
            return SearchPage([self._problem(row) for row in rows], int(rows[0].total))
        # a page past the end has no rows to carry the total, so count separately (only then)
        return SearchPage([], self._count(where, params) if query.page > 0 else 0)

    def tag_counts(self) -> List[Dict[str, Any]]:
        """Every tag with how many problems carry it. Tags that differ only in capitals count as one."""
        rows = self.db.execute(text(f"""
            SELECT MIN(t) AS tag, COUNT(DISTINCT p.id) AS problems
            FROM problems p, unnest({_TAGS}) AS t
            WHERE t <> ''
            GROUP BY LOWER(t)
            ORDER BY problems DESC, LOWER(t)
        """)).fetchall()
        return [{"tag": row.tag, "count": int(row.problems)} for row in rows]

    # ---- internals ----

    def _filters(self, query: SearchQuery) -> Tuple[str, Dict[str, Any]]:
        clauses: List[str] = []
        params: Dict[str, Any] = {}

        if query.difficulty:
            clauses.append("LOWER(p.difficulty) = :difficulty")
            params["difficulty"] = query.difficulty

        if query.tags:
            params["tags"] = query.tags
            matching = f"SELECT 1 FROM unnest({_TAGS}) AS t WHERE LOWER(t) = ANY(CAST(:tags AS text[]))"
            if query.tag_mode == options.TAG_MODE_ALL:
                clauses.append(
                    f"(SELECT COUNT(DISTINCT LOWER(t)) FROM unnest({_TAGS}) AS t "
                    f"WHERE LOWER(t) = ANY(CAST(:tags AS text[]))) = :tag_count")
                params["tag_count"] = len(query.tags)
            else:
                clauses.append(f"EXISTS ({matching})")

        if query.text:
            clauses.append(_TEXT_MATCH)
            params.update(search=query.text, like=f"%{escape_like(query.text)}%", prefix=f"{escape_like(query.text)}%")

        return ("WHERE " + " AND ".join(clauses)) if clauses else "", params

    def _order_by(self, query: SearchQuery) -> str:
        if query.sort == options.SORT_RELEVANCE:
            # best match first when searching; with nothing to match against, the order problems were added in
            return f"{_RELEVANCE}, {_SUBMISSIONS} DESC, p.id" if query.text else "p.id"
        return _ORDER_BY[query.sort]

    def _count(self, where: str, params: Dict[str, Any]) -> int:
        return int(self.db.execute(text(f"SELECT COUNT(*) FROM problems p {where}"), params).scalar() or 0)

    @staticmethod
    def _problem(row) -> Dict[str, Any]:
        submissions, accepted = int(row.submissions), int(row.accepted)
        return {
            "id": row.id,
            "title": row.title,
            "tags": row.tags or [],
            "difficulty": row.difficulty,
            "submissions": submissions,
            "solvedBy": int(row.solvers),
            # None until somebody has submitted: "0%" would claim the problem is unsolvable
            "acceptance": round(100.0 * accepted / submissions, 1) if submissions else None,
        }
