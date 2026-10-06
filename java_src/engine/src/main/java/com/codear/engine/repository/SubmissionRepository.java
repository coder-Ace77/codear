package com.codear.engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.codear.engine.entity.Submission;
import com.codear.engine.enums.RunStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findBySubmissionId(String submissionId);

    /**
     * Records the verdict, but only for a submission that is still IN_PROGRESS: the queue can deliver a message
     * twice, and the second judging must not overwrite the first.
     *
     * A declared @Modifying query gets no transaction from Spring Data by default, and without one Hibernate
     * refuses to run it ("Executing an update/delete query"), so every verdict would fail to save.
     */
    @Transactional
    @Modifying
    @Query("""
                UPDATE Submission s
                SET s.status = :status,
                    s.verdict = :verdict,
                    s.failedTest = :failedTest,
                    s.result = :result,
                    s.errorLog = :errorLog,
                    s.totalTests = :totalTests,
                    s.passedTests = :passedTests,
                    s.timeTakenMs = :timeTakenMs,
                    s.memoryUsed = :memoryUsed
                WHERE s.submissionId = :submissionId
                  AND s.status = com.codear.engine.enums.RunStatus.IN_PROGRESS
            """)
    int finishSubmission(
            @Param("submissionId") String submissionId,
            @Param("status") RunStatus status,
            @Param("verdict") String verdict,
            @Param("failedTest") Integer failedTest,
            @Param("result") String result,
            @Param("errorLog") String errorLog,
            @Param("totalTests") Integer totalTests,
            @Param("passedTests") Integer passedTests,
            @Param("timeTakenMs") Long timeTakenMs,
            @Param("memoryUsed") String memoryUsed);

    @Query("SELECT count(s.problemId) from Submission s where s.problemId=:problemId and s.userId=:userId")
    int countSolved(@Param("problemId") Long problemId, @Param("userId") Long userId);

    @Query("SELECT MAX(s.submittedAt) FROM Submission s WHERE s.userId = :userId")
    LocalDateTime findLastSubmissionTime(@Param("userId") Long userId);

}
