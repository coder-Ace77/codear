package com.codear.problem.repository;

import com.codear.problem.entity.Problem;
import com.codear.problem.dto.ProblemSummaryDTO; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <-- 1. MAKE SURE YOU IMPORT THIS
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    /**
     * 2. ADD THIS ANNOTATION
     * This query efficiently fetches *only* the id, title, and tags
     * and constructs a ProblemSummaryDTO for each row directly.
     */
    @Query("SELECT new com.codear.problem.dto.ProblemSummaryDTO(p.id, p.title, p.tags,p.difficulty) FROM Problem p")
    List<ProblemSummaryDTO> findAllSummaries();

}