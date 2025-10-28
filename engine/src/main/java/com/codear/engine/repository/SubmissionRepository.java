package com.codear.engine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.codear.engine.entity.Submission;
import com.codear.engine.enums.RunStatus;

import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findBySubmissionId(String submissionId);

    @Modifying
    @Query("""
        UPDATE Submission s 
        SET s.status = :status,
            s.result = :result,
            s.totalTests = :totalTests,
            s.passedTests = :passedTests,
            s.timeTakenMs = COALESCE(s.timeTakenMs, 0),
            s.memoryUsed = COALESCE(s.memoryUsed, '0MB')
        WHERE s.submissionId = :submissionId
    """)
    int updateSubmissionResult(
        @Param("submissionId") String submissionId,
        @Param("status") RunStatus status,
        @Param("result") String result,
        @Param("totalTests") Integer totalTests,
        @Param("passedTests") Integer passedTests);

}
