package com.codear.engine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.codear.engine.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    
}
