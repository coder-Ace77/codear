package com.codear.problem.repository;

import com.codear.problem.entity.Problem;
import com.codear.problem.dto.ProblemSendDTO;
import com.codear.problem.dto.ProblemSummaryDTO; 
import org.springframework.data.domain.Pageable; // <-- 1. IMPORT THIS
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; 
import org.springframework.data.repository.query.Param; // <-- 2. IMPORT THIS
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    @Query("SELECT new com.codear.problem.dto.ProblemSummaryDTO(p.id, p.title, p.tags, p.difficulty) FROM Problem p")
    List<ProblemSummaryDTO> findAllSummaries();

    @Query("""
    SELECT new com.codear.problem.dto.ProblemSendDTO(
        p.id, p.title, p.description,
        p.inputDescription, p.outputDescription,
        p.constraints, p.difficulty,
        p.tags, p.timeLimitMs, p.memoryLimitMb
    )
    FROM Problem p
    WHERE p.id = :id""")
    ProblemSendDTO findProblemByIdOnly(Long id);

    /**
     * 3. ADD THIS NEW METHOD
     *
     * This query:
     * 1. Joins Problem (p) with Submission (s) on the problem ID.
     * 2. Filters submissions for a specific 'userId'.
     * 3. Groups results by problem (p.id and all other selected fields) to get unique problems.
     * 4. Orders these groups by the most recent submission time (MAX(s.submittedAt) DESC).
     * 5. Uses the 'Pageable' parameter to limit the result to the top 5.
     * 6. Constructs the 'ProblemSummaryDTO' for the matching problems.
     */
    @Query("""
        SELECT new com.codear.problem.dto.ProblemSummaryDTO(p.id, p.title, p.tags, p.difficulty)
        FROM Problem p
        JOIN com.codear.problem.dto.Submission s ON p.id = s.problemId
        WHERE s.userId = :userId
        GROUP BY p.id, p.title, p.tags, p.difficulty
        ORDER BY MAX(s.submittedAt) DESC
    """)
    List<ProblemSummaryDTO> findRecentProblemSummariesByUserId(
        @Param("userId") Long userId, 
        Pageable pageable
    );


    @Query(value = """
        SELECT 
            p.id, 
            p.title, 
            p.tags, 
            p.difficulty
        FROM problems p
        WHERE
            (COALESCE(:search, '') = '' OR 
                to_tsvector('english', p.title || ' ' || p.description) @@ plainto_tsquery(:search))
            AND (COALESCE(:difficulty, '') = '' OR LOWER(p.difficulty) = LOWER(:difficulty))
            AND (COALESCE(:tags, '') = '' OR p.tags && string_to_array(:tags, ','))
        ORDER BY p.id
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> searchProblemsNative(
            @Param("search") String search,
            @Param("difficulty") String difficulty,
            @Param("tags") String tags,
            @Param("limit") int limit,
            @Param("offset") Long offset
    );

    @Query(value = """
        SELECT COUNT(*) 
        FROM problems p
        WHERE
            (COALESCE(:search, '') = '' OR 
                to_tsvector('english', p.title || ' ' || p.description) @@ plainto_tsquery(:search))
            AND (COALESCE(:difficulty, '') = '' OR LOWER(p.difficulty) = LOWER(:difficulty))
            AND (COALESCE(:tags, '') = '' OR p.tags && string_to_array(:tags, ','))
    """, nativeQuery = true)
    long countFilteredProblems(
            @Param("search") String search,
            @Param("difficulty") String difficulty,
            @Param("tags") String tags
    );

}