package com.codear.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codear.problem.dto.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
}
