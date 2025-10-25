package com.codear.engine.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codear.engine.entity.TestCase;
import com.codear.engine.repository.TestCaseRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProblemCrudService {
    private final TestCaseRepository testCaseRepository;

    List<TestCase> getAllTestCases(Long problemId){
        return testCaseRepository.findByProblemId(problemId);
    }
}
