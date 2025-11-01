package com.codear.problem.repository;

// Make sure this is your @Entity class, not a DTO
import com.codear.problem.dto.Submission; 
// Correct @Param import
import org.springframework.data.repository.query.Param; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    
    Optional<Submission> findBySubmissionId(String submissionId);
    
    List<Submission> findByProblemId(Long problemId);

    @Query("select s from Submission s where s.problemId = :problemId and s.userId = :userId")
    List<Submission> getSubmissionByIdAndProblem(@Param("problemId") Long problemId, @Param("userId") Long userId);
}