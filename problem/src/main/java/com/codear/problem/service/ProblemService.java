package com.codear.problem.service;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.PageRequest;
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
    private static final String PROBLEM_SEARCH_KEY = "Search_problem";

    @Transactional 
    public Problem addProblem(Problem problem) {
        if (problem.getTestCases() != null) {
            problem.getTestCases().forEach(testCase -> testCase.setProblem(problem));
        }        
        Problem savedProblem = problemRepository.save(problem);
        cacheService.deleteKey(ALL_PROBLEMS_SUMMARY_KEY);
        cacheService.deleteKey(PROBLEM_COUNT_KEY);
        return savedProblem;
    }

    public ProblemSendDTO getProblemById(Long id) {
        String key = PROBLEM_KEY_PREFIX + id;
        ProblemSendDTO cachedProblem = cacheService.getObjectValue(key, ProblemSendDTO.class);
        if (cachedProblem != null) {
            return cachedProblem;
        }
        ProblemSendDTO dbProblem = problemRepository.findProblemByIdOnly(id);
        if (dbProblem != null) {
            cacheService.setObjectValue(key, dbProblem, 1, TimeUnit.HOURS);
        }
        return dbProblem;
    }

    public List<ProblemSummaryDTO> getAllProblems() {
        List<ProblemSummaryDTO> cachedList = cacheService.getObjectListValue(ALL_PROBLEMS_SUMMARY_KEY, ProblemSummaryDTO.class);
        if (cachedList != null) {
            return cachedList;
        }
        List<ProblemSummaryDTO> dbList = problemRepository.findAllSummaries();
        cacheService.setObjectValue(ALL_PROBLEMS_SUMMARY_KEY, dbList, 30, TimeUnit.MINUTES);
        return dbList;
    }

    @Deprecated
    public List<String> getTags(){
        List<String> cachedTags = cacheService.getObjectListValue(ALL_TAGS_KEY, String.class);
        if (cachedTags != null) {
            return cachedTags;
        }
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

    public List<ProblemSummaryDTO> getProblemSummaryRecent(Long userId) {
        Pageable topFive = PageRequest.of(0, 5);
        return problemRepository.findRecentProblemSummariesByUserId(userId, topFive);
    }

    public List<ProblemSummaryDTO> searchProblems(
            String search, String difficulty, List<String> tags,
            int page, int size
    ) {
        String tagString = (tags != null && !tags.isEmpty()) ? String.join(",", tags) : null;

        Long lastSeenId = (long) (page * size);

        String key = String.format("%s:search=%s:difficulty=%s:tags=%s:lastId=%s:size=%d",
                PROBLEM_SEARCH_KEY,
                (search == null ? "none" : search),
                (difficulty == null ? "none" : difficulty),
                (tagString == null ? "none" : tagString),
                (lastSeenId == null ? "none" : lastSeenId),
                size
        );

        System.out.println(search + " " + difficulty + " " + tagString + " " + lastSeenId);

        List<ProblemSummaryDTO> cached = cacheService.getObjectListValue(key, ProblemSummaryDTO.class);
        if (cached != null) {
            System.out.println("✅ Cache hit for " + key);
            return cached;
        }

        System.out.println("🧭 Cache miss. Querying DB for: " + key);

        List<Object[]> rows = problemRepository.searchProblemsNative(search, difficulty, tagString, size, lastSeenId);

        List<ProblemSummaryDTO> result = rows.stream().map(row -> {
            Long id = ((Number) row[0]).longValue();
            String title = (String) row[1];
            List<String> tagList = Collections.emptyList();
            if (row[2] != null) {
                if (row[2] instanceof java.sql.Array sqlArray) {
                    try {
                        String[] array = (String[]) sqlArray.getArray();
                        tagList = Arrays.asList(array);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else if (row[2] instanceof String[]) {
                    tagList = Arrays.asList((String[]) row[2]);
                } else {
                    // fallback for unexpected string format like "{tag1,tag2}"
                    String tagsStr = row[2].toString().replaceAll("[{}\"]", "");
                    tagList = tagsStr.isEmpty() ? List.of() : List.of(tagsStr.split(","));
                }
            }

            String diff = (String) row[3];

            return new ProblemSummaryDTO(id, title, tagList, diff);
        }).toList();

        System.out.println("result in prob service " + result);

        cacheService.setObjectValue(key, result, 30, TimeUnit.MINUTES);
        return result;
    }

    public long countFilteredProblems(String search, String difficulty, List<String> tags) {
        String tagString = (tags != null && !tags.isEmpty()) ? String.join(",", tags) : null;
        return problemRepository.countFilteredProblems(search, difficulty, tagString);
    }

    
}