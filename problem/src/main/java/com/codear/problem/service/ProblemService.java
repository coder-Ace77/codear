package com.codear.problem.service;

import java.util.List;
import java.util.concurrent.TimeUnit; 

import org.springframework.stereotype.Service;
import com.codear.problem.repository.ProblemRepository;
import com.codear.problem.dto.ProblemSendDTO;
import com.codear.problem.dto.ProblemSummaryDTO;
import com.codear.problem.entity.Problem;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final CacheService cacheService; 

    private static final String PROBLEM_KEY_PREFIX = "problem:id:";
    private static final String ALL_PROBLEMS_SUMMARY_KEY = "all_problems_summary";
    private static final String ALL_TAGS_KEY = "all_tags";
    private static final String PROBLEM_COUNT_KEY = "problem_count";

    @Transactional 
    public Problem addProblem(Problem problem) {
        if (problem.getTestCases() != null) {
            problem.getTestCases().forEach(testCase -> testCase.setProblem(problem));
        }        
        Problem savedProblem = problemRepository.save(problem);

        System.out.println("Invalidating caches for all_problems_summary and problem_count");
        cacheService.deleteKey(ALL_PROBLEMS_SUMMARY_KEY);
        cacheService.deleteKey(PROBLEM_COUNT_KEY);

        return savedProblem;
    }

    public ProblemSendDTO getProblemById(Long id) {
        String key = PROBLEM_KEY_PREFIX + id;
        
        ProblemSendDTO cachedProblem = cacheService.getObjectValue(key, ProblemSendDTO.class);
        if (cachedProblem != null) {
            System.out.println("Fetching problem " + id + " from CACHE");
            return cachedProblem;
        }

        System.out.println("Fetching problem " + id + " from DB");
        ProblemSendDTO dbProblem = problemRepository.findProblemByIdOnly(id);

        if (dbProblem != null) {
            cacheService.setObjectValue(key, dbProblem, 1, TimeUnit.HOURS);
        }
        
        return dbProblem;
    }

    public List<ProblemSummaryDTO> getAllProblems() {
        List<ProblemSummaryDTO> cachedList = cacheService.getObjectListValue(ALL_PROBLEMS_SUMMARY_KEY, ProblemSummaryDTO.class);
        if (cachedList != null) {
            System.out.println("Finding summaries from CACHE");
            return cachedList;
        }

        System.out.println("Finding summaries from DB");
        List<ProblemSummaryDTO> dbList = problemRepository.findAllSummaries();

        cacheService.setObjectValue(ALL_PROBLEMS_SUMMARY_KEY, dbList, 30, TimeUnit.MINUTES);
        
        return dbList;
    }

    public List<String> getTags(){
        List<String> cachedTags = cacheService.getObjectListValue(ALL_TAGS_KEY, String.class);
        if (cachedTags != null) {
            System.out.println("Fetching tags from CACHE");
            return cachedTags;
        }

        System.out.println("Generating tags list");
        List<String> tags = List.of("Strings","Maths","DP","Graph");

        cacheService.setObjectValue(ALL_TAGS_KEY, tags, 24, TimeUnit.HOURS);
        
        return tags;
    }

    public Long getProblemCnt(){
        String cachedCountStr = cacheService.getValue(PROBLEM_COUNT_KEY);
        if (cachedCountStr != null) {
            System.out.println("Fetching problem count from CACHE");
            try {
                return Long.parseLong(cachedCountStr);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse cached count: " + cachedCountStr);
            }
        }

        System.out.println("Fetching problem count from DB");
        Long dbCount = problemRepository.count();

        cacheService.setValue(PROBLEM_COUNT_KEY, dbCount.toString(), 30, TimeUnit.MINUTES);
        
        return dbCount;
    }
    
}