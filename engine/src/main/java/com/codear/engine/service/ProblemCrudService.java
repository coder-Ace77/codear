package com.codear.engine.service;

import java.util.List;
import java.util.concurrent.TimeUnit; 

import org.springframework.stereotype.Service;

import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.entity.TestCase;
import com.codear.engine.repository.ProblemRepository;
import com.codear.engine.repository.TestCaseRepository;


import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProblemCrudService {
    
    private final TestCaseRepository testCaseRepository;
    private final CacheService cacheService; 
    private final ProblemRepository problemRepository;

    private static final String TESTCASES_KEY_PREFIX = "testcases:problem:";
    private static final String CONSTRAINTS_KEY_PREFIX = "constraints:problem";

    List<TestCase> getAllTestCases(Long problemId){
        String key = TESTCASES_KEY_PREFIX + problemId;
        
        List<TestCase> cachedList = cacheService.getObjectListValue(key, TestCase.class);
        if (cachedList != null) {
            return cachedList;
        }
        List<TestCase> dbList = testCaseRepository.findByProblemId(problemId);
        cacheService.setObjectValue(key, dbList, 1, TimeUnit.HOURS);
        return dbList;
    }

    public ResourceConstraints getPromblemConstraints(Long problemId){
        String key=CONSTRAINTS_KEY_PREFIX+problemId;
        ResourceConstraints cachedResourceConstraints = cacheService.getObjectValue(key,ResourceConstraints.class);
        if(cachedResourceConstraints!=null){
            return cachedResourceConstraints;
        }
        ResourceConstraints resourceConstraints = problemRepository.findConstraintsById(problemId).orElse(null);
        cacheService.setObjectValue(key, resourceConstraints);
        return resourceConstraints;
    }
}