package com.codear.problem.repository;

import com.codear.problem.dto.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findBySubmissionId(String submissionId);

    List<Submission> findByProblemId(Long problemId);
}
