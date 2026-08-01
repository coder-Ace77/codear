from sqlalchemy.orm import Session
from sqlalchemy import func, desc, text
from app.models.problem import Problem, Submission, TestCase, Editorial
from app.schemas.problem_schema import ProblemDTO, ProblemSendDTO, ProblemSummaryDTO
from app.services.cache_service import CacheService
from app.core.local_cache import LocalCache
from typing import List, Optional
import json
import time
import logging
from functools import wraps

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def profile_time(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        end_time = time.time()
        logger.info(f"Execution time for {func.__name__}: {end_time - start_time:.4f} seconds")
        return result
    return wrapper

class ProblemService:
    def __init__(self, db: Session):
        self.db = db
        # Cache Keys matching your Java service constants
        self.PROBLEM_KEY_PREFIX = "problem:id:"
        self.ALL_TAGS_KEY = "all_tags"
        self.PROBLEM_COUNT_KEY = "problem_count"
        self.PROBLEM_SEARCH_KEY = "Search_problem"

    def add_problem(self, problem_dto: ProblemDTO) -> Problem:
        # 1. Initialize the Problem Model 
        # Use the camelCase names defined in your ProblemDTO
        db_problem = Problem(
            title=problem_dto.title,
            description=problem_dto.description,
            input_description=problem_dto.inputDescription, # Use DTO name
            output_description=problem_dto.outputDescription, # Use DTO name
            constraints=problem_dto.constraints,
            difficulty=problem_dto.difficulty,
            tags=problem_dto.tags,
            time_limit_ms=problem_dto.timeLimitMs, # Use DTO name
            memory_limit_mb=problem_dto.memoryLimitMb  # Use DTO name
        )

        # 2. Handle nested TestCases
        # Fix: problem_dto.testCases (NOT test_cases)
        if problem_dto.testCases:
            db_problem.test_cases = [
                TestCase(
                    input=tc.input,
                    output=tc.output,
                    is_sample=tc.isSample # Use tc.isSample from TestCaseDTO
                ) for tc in problem_dto.testCases
            ]

        try:
            self.db.add(db_problem)
            self.db.commit()
            self.db.refresh(db_problem)

            # 4. Clear Cache Keys
            CacheService.delete("all_problems_summary")
            # Ensure PROBLEM_COUNT_KEY is defined in your class or passed correctly
            CacheService.delete(self.PROBLEM_COUNT_KEY) 
            
            # Clear Local Cache
            LocalCache.delete(self.PROBLEM_COUNT_KEY)
            LocalCache.invalidate_prefix(self.PROBLEM_SEARCH_KEY) # Invalidate all search results
            LocalCache.delete(self.ALL_TAGS_KEY)

            # Clear Redis Search Keys
            CacheService.delete_pattern(f"{self.PROBLEM_SEARCH_KEY}*")
            CacheService.delete(self.ALL_TAGS_KEY)
            
            return db_problem
        except Exception as e:
            self.db.rollback()
            print(f"Error adding problem: {e}")
            raise e

    @profile_time
    def get_problem_by_id(self, problem_id: int):
        key = f"{self.PROBLEM_KEY_PREFIX}{problem_id}"
        
        # 1. Try Cache
        cached_problem = CacheService.get_object(key)
        if cached_problem and "testCases" in cached_problem:
            return cached_problem
            
        # 2. Query DB
        # Use joinedload to eagerly load test_cases if not already handled by lazy loading efficiently enough
        # But here we filter in Python for simplicity as the number of TCs isn't massive, just their content could be.
        # Alternatively, we could filter in the query itself:
        # db_problem = self.db.query(Problem).options(joinedload(Problem.test_cases)).filter(Problem.id == problem_id).first()
        
        db_problem = self.db.query(Problem).filter(Problem.id == problem_id).first()
        
        if not db_problem:
            return None
            
        # 3. Map to DTO format
        # Filter test cases: Only return IS_SAMPLE ones
        public_test_cases = [tc for tc in db_problem.test_cases if tc.is_sample]

        problem_data = {
            "id": db_problem.id,
            "title": db_problem.title,
            "description": db_problem.description,
            "inputDescription": db_problem.input_description,
            "outputDescription": db_problem.output_description,
            "constraints": db_problem.constraints,
            "difficulty": db_problem.difficulty,
            "tags": db_problem.tags or [],
            "timeLimitMs": db_problem.time_limit_ms,
            "memoryLimitMb": db_problem.memory_limit_mb,
            "testCases": [
                {
                    "id": tc.id,
                    "input": tc.input,
                    "output": tc.output,
                    "isSample": tc.is_sample
                } for tc in public_test_cases
            ]
        }
        
        # 4. Set Cache
        CacheService.set_object(key, problem_data, expire_seconds=3600)
        return problem_data

    @profile_time
    def get_all_problems(self) -> List[dict]:
        """Equivalent to Java findAllSummaries."""
        db_list = self.db.query(Problem.id, Problem.title, Problem.tags, Problem.difficulty).all()
        return [
            {"id": p.id, "title": p.title, "tags": p.tags or [], "difficulty": p.difficulty} 
            for p in db_list
        ]

    @profile_time
    def get_problem_cnt(self) -> int:
        # L1: Local Cache
        local_count = LocalCache.get(self.PROBLEM_COUNT_KEY)
        if local_count is not None:
            return int(local_count)

        # L2: Redis Cache
        cached_count = CacheService.get_value(self.PROBLEM_COUNT_KEY)
        if cached_count:
            # Populate L1
            LocalCache.set(self.PROBLEM_COUNT_KEY, int(cached_count), ttl=None)
            return int(cached_count)
            
        count = self.db.query(Problem).count()
        
        # Set L2 and L1
        CacheService.set_object(self.PROBLEM_COUNT_KEY, count, expire_seconds=1800)
        LocalCache.set(self.PROBLEM_COUNT_KEY, count, ttl=None)
        return count

    @profile_time
    def get_tags_for_problem(self) -> List[str]:
        # L1
        local_tags = LocalCache.get(self.ALL_TAGS_KEY)
        if local_tags:
            return local_tags

        # L2
        cached_tags = CacheService.get_object(self.ALL_TAGS_KEY)
        if cached_tags:
            LocalCache.set(self.ALL_TAGS_KEY, cached_tags, ttl=None)
            return cached_tags
            
        query = text("SELECT DISTINCT UNNEST(tags) as tag FROM problems WHERE tags IS NOT NULL")
        result = self.db.execute(query).fetchall()
        tags = sorted([r.tag for r in result if r.tag])
        
        CacheService.set_object(self.ALL_TAGS_KEY, tags, expire_seconds=86400)
        LocalCache.set(self.ALL_TAGS_KEY, tags, ttl=None)
        return tags

    @profile_time
    def search_problems(self, search: Optional[str], difficulty: Optional[str], tags: Optional[List[str]], page: int, size: int):
        tags_str = ",".join(sorted(tags)) if tags else "None"
        cache_key = f"{self.PROBLEM_SEARCH_KEY}:{search or 'None'}:{difficulty or 'None'}:{tags_str}:{page}:{size}"
        
        # L1
        local_result = LocalCache.get(cache_key)
        if local_result:
            return local_result

        # L2
        cached_result = CacheService.get_object(cache_key)
        if cached_result:
            LocalCache.set(cache_key, cached_result, ttl=None)
            return cached_result

        offset = page * size
        tag_str = ",".join(tags) if tags else None
        
        query = text("""
            SELECT id, title, tags, difficulty FROM problems p
            WHERE (:search IS NULL OR :search = '' OR to_tsvector('english', p.title || ' ' || p.description) @@ plainto_tsquery(:search))
            AND (:difficulty IS NULL OR :difficulty = '' OR LOWER(p.difficulty) = LOWER(:difficulty))
            AND (:tags IS NULL OR :tags = '' OR p.tags && string_to_array(:tags, ','))
            ORDER BY p.id LIMIT :limit OFFSET :offset
        """)
        
        result = self.db.execute(query, {
            "search": search, "difficulty": difficulty, 
            "tags": tag_str, "limit": size, "offset": offset
        }).fetchall()
        
        data = [{"id": r.id, "title": r.title, "tags": r.tags or [], "difficulty": r.difficulty} for r in result]
        CacheService.set_object(cache_key, data, expire_seconds=300)
        LocalCache.set(cache_key, data, ttl=None)
        return data

    @profile_time
    def count_filtered_problems(self, search: Optional[str], difficulty: Optional[str], tags: Optional[List[str]]):
        tags_str = ",".join(sorted(tags)) if tags else "None"
        cache_key = f"{self.PROBLEM_SEARCH_KEY}_count:{search or 'None'}:{difficulty or 'None'}:{tags_str}"
        
        # L1
        local_count = LocalCache.get(cache_key)
        if local_count is not None:
             return int(local_count)

        # L2
        cached_count = CacheService.get_value(cache_key)
        if cached_count is not None:
            LocalCache.set(cache_key, int(cached_count), ttl=None)
            return int(cached_count)

        tag_str = ",".join(tags) if tags else None
        query = text("""
            SELECT COUNT(*) FROM problems p
            WHERE (:search IS NULL OR :search = '' OR to_tsvector('english', p.title || ' ' || p.description) @@ plainto_tsquery(:search))
            AND (:difficulty IS NULL OR :difficulty = '' OR LOWER(p.difficulty) = LOWER(:difficulty))
            AND (:tags IS NULL OR :tags = '' OR p.tags && string_to_array(:tags, ','))
        """)
        count = self.db.execute(query, {"search": search, "difficulty": difficulty, "tags": tag_str}).scalar()
        CacheService.set_object(cache_key, count, expire_seconds=300)
        LocalCache.set(cache_key, count, ttl=None)
        return count

    def get_problem_summary_recent(self, user_id: int):
        results = (
            self.db.query(
                Problem.id, 
                Problem.title, 
                Problem.tags, 
                Problem.difficulty
            )
            .join(Submission, Problem.id == Submission.problem_id)
            .filter(Submission.user_id == user_id)
            .group_by(Problem.id, Problem.title, Problem.tags, Problem.difficulty)
            .order_by(desc(func.max(Submission.submitted_at)))
            .limit(5)
            .all()
        )
        return [
            {"id": r.id, "title": r.title, "tags": r.tags or [], "difficulty": r.difficulty}
            for r in results
        ]

    def delete_problem(self, problem_id: int):
        # 1. Get Problem
        problem = self.db.query(Problem).filter(Problem.id == problem_id).first()
        if not problem:
            return False
            
        # 2. Delete Dependencies & Problem
        try:
            # Delete Editorials (FK Constraint)
            self.db.query(Editorial).filter(Editorial.problem_id == problem_id).delete()
            
            # Delete Submissions (Cleanup)
            self.db.query(Submission).filter(Submission.problem_id == problem_id).delete()
            
            # Delete Problem (TestCases cascade automatically)
            self.db.delete(problem)
            self.db.commit()
            
            # 3. Clear Caches
            key = f"{self.PROBLEM_KEY_PREFIX}{problem_id}"
            CacheService.delete(key)
            CacheService.delete("all_problems_summary")
            CacheService.delete(self.PROBLEM_COUNT_KEY)
            CacheService.delete(self.ALL_TAGS_KEY)
            CacheService.delete_pattern(f"{self.PROBLEM_SEARCH_KEY}*")
            
            LocalCache.delete(self.PROBLEM_COUNT_KEY)
            LocalCache.invalidate_prefix(self.PROBLEM_SEARCH_KEY)
            LocalCache.delete(self.ALL_TAGS_KEY) # Tags might change
            
            return True
        except Exception as e:
            self.db.rollback()
            print(f"Error deleting problem: {e}")
            raise e