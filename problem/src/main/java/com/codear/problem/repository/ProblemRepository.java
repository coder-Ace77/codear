package com.codear.problem.repository;

import com.codear.problem.dto.ProblemSendDTO;
import com.codear.problem.dto.ProblemSummaryDTO;
import com.codear.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    // ✅ 1️⃣ Fetch all problems in lightweight summary form
    @Query("""
        SELECT new com.codear.problem.dto.ProblemSummaryDTO(
            p.id, p.title, p.tags, p.difficulty
        )
        FROM Problem p
    """)
    List<ProblemSummaryDTO> findAllSummaries();


    // ✅ 2️⃣ Fetch full problem details for a given ID
    @Query("""
        SELECT new com.codear.problem.dto.ProblemSendDTO(
            p.id, p.title, p.description,
            p.inputDescription, p.outputDescription,
            p.constraints, p.difficulty,
            p.tags, p.timeLimitMs, p.memoryLimitMb
        )
        FROM Problem p
        WHERE p.id = :id
    """)
    ProblemSendDTO findProblemByIdOnly(@Param("id") Long id);


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
            @Param("offset") int offset
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



    // ✅ 4️⃣ Count query for pagination support
//     @Query(value = """
//         SELECT COUNT(*)
//         FROM problems p
//         WHERE
//             (COALESCE(:search, '') = '' OR 
//                 to_tsvector('english', p.title || ' ' || p.description) @@ plainto_tsquery(:search))
//             AND (COALESCE(:difficulty, '') = '' OR LOWER(p.difficulty) = LOWER(:difficulty))
//             AND (COALESCE(:tags, '') = '' OR p.tags && string_to_array(:tags, ','))
//     """, nativeQuery = true)
//     long countFilteredProblems(
//         @Param("search") String search,
//         @Param("difficulty") String difficulty,
//         @Param("tags") String tags
//     );
}
