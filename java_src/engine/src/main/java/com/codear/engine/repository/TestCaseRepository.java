package com.codear.engine.repository;

import com.codear.engine.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    /** In a fixed order, so "the first failing test" means the same thing every time. */
    List<TestCase> findByProblemIdOrderByIdAsc(Long problemId);
}
