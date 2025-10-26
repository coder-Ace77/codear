package com.codear.engine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.codear.engine.entity.Submission;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findBySubmissionId(String submissionId);
}
