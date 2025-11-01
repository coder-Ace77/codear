package com.codear.problem.repository;

import com.codear.problem.entity.Problem;
import com.codear.problem.dto.ProblemSendDTO;
import com.codear.problem.dto.ProblemSummaryDTO; 
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; 
import org.springframework.data.repository.query.Param;
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
}