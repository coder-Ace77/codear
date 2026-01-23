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
    private final CacheService cacheService;
    private final ProblemRepository problemRepository;
    private final LocalCacheService localCacheService;

    private static final String TESTCASES_KEY_PREFIX = "testcases:problem:";
    private static final String CONSTRAINTS_KEY_PREFIX = "constraints:problem";

    List<TestCase> getAllTestCases(Long problemId) {
        org.springframework.util.StopWatch stopWatch = new org.springframework.util.StopWatch(
                "ProblemCrudService.getAllTestCases");
        try {
            String key = TESTCASES_KEY_PREFIX + problemId;

            stopWatch.start("local-cache-get");
            List<TestCase> localCachedList = localCacheService.get(key);
            stopWatch.stop();

            if (localCachedList != null) {
                return localCachedList;
            }

            stopWatch.start("db-get");
            List<TestCase> dbList = testCaseRepository.findByProblemId(problemId);
            stopWatch.stop();

            stopWatch.start("local-cache-set");
            localCacheService.put(key, dbList);
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

        // Use local cache for constraints too
        ResourceConstraints cachedResourceConstraints = localCacheService.get(key);
        if (cachedResourceConstraints != null) {
            return cachedResourceConstraints;
        }

        // Bypassing Redis as per instruction to use local cache
        ResourceConstraints resourceConstraints = problemRepository.findConstraintsById(problemId).orElse(null);

        if (resourceConstraints != null) {
            localCacheService.put(key, resourceConstraints);
        }

        return resourceConstraints;
    }
}