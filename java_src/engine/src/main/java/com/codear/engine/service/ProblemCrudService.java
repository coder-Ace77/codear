package com.codear.engine.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.entity.TestCase;
import com.codear.engine.repository.ProblemRepository;
import com.codear.engine.repository.TestCaseRepository;
import com.codear.engine.util.CacheService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProblemCrudService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;
    private final CacheService cacheService;

    private static final String TESTCASES_KEY_PREFIX = "testcases:problem:";
    private static final String CONSTRAINTS_KEY_PREFIX = "constraints:problem";

    List<TestCase> getAllTestCases(Long problemId) {
        
        String key = TESTCASES_KEY_PREFIX + problemId;
        List<TestCase> cachedList = cacheService.getList(key, TestCase.class);
        if (cachedList != null){
            return cachedList;
        }
        List<TestCase> dbList = testCaseRepository.findByProblemIdOrderByIdAsc(problemId);
        cacheService.set(key, dbList);
        return dbList;

    }

    public ResourceConstraints getPromblemConstraints(Long problemId){
        String key = CONSTRAINTS_KEY_PREFIX + problemId;

        ResourceConstraints cachedResourceConstraints = cacheService.get(key, ResourceConstraints.class);
        
        if (cachedResourceConstraints != null) {
            return cachedResourceConstraints;
        }
        ResourceConstraints resourceConstraints = problemRepository.findConstraintsById(problemId).orElse(null);
        cacheService.set(key, resourceConstraints);        
        return resourceConstraints;
    }
}