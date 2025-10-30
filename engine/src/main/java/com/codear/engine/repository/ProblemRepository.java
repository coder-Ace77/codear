package com.codear.engine.repository;

import com.codear.engine.entity.Problem; 
import com.codear.engine.dto.ResourceConstraints; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; 
import java.util.Optional; 

public interface ProblemRepository extends JpaRepository<Problem, Long>{
        
    @Query("SELECT new com.codear.engine.dto.ResourceConstraints(p.timeLimitMs, p.memoryLimitMb,p.difficulty) FROM Problem p WHERE p.id = :problemId")
    Optional<ResourceConstraints> findConstraintsById(Long problemId);
}