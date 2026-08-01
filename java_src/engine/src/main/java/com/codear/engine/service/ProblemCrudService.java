package com.codear.engine.service;

import java.util.List;

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
    private final ProblemRepository problemRepository;
    private final CacheService cacheService;

    private static final String TESTCASES_KEY_PREFIX = "testcases:problem:";
    private static final String CONSTRAINTS_KEY_PREFIX = "constraints:problem";

    List<TestCase> getAllTestCases(Long problemId) {
        org.springframework.util.StopWatch stopWatch = new org.springframework.util.StopWatch(
                "ProblemCrudService.getAllTestCases");
        try {
            String key = TESTCASES_KEY_PREFIX + problemId;
            stopWatch.start("redis-cache-get");
            List<TestCase> cachedList = cacheService.getObjectListValue(key, TestCase.class);
            stopWatch.stop();

            if (cachedList != null) {
                return cachedList;
            }

            stopWatch.start("db-get");
            List<TestCase> dbList = testCaseRepository.findByProblemId(problemId);
            stopWatch.stop();

            stopWatch.start("redis-cache-set");
            cacheService.setObjectValue(key, dbList);
            stopWatch.stop();

            return dbList;
        } finally {
            if (stopWatch.isRunning())
                stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

    public ResourceConstraints getPromblemConstraints(Long problemId) {
        String key = CONSTRAINTS_KEY_PREFIX + problemId;
        ResourceConstraints cachedResourceConstraints = cacheService.getObjectValue(key, ResourceConstraints.class);
        if (cachedResourceConstraints != null) {
            return cachedResourceConstraints;
        }
        ResourceConstraints resourceConstraints = problemRepository.findConstraintsById(problemId).orElse(null);
        if (resourceConstraints != null) {
            cacheService.setObjectValue(key, resourceConstraints);
        }
        return resourceConstraints;
    }
}