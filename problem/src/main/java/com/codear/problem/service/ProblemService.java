package com.codear.problem.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com.codear.problem.repository.ProblemRepository;
import com.codear.problem.dto.ProblemSummaryDTO;
import com.codear.problem.entity.Problem;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;

    @Transactional 
    public Problem addProblem(Problem problem) {
        if (problem.getTestCases() != null) {
            problem.getTestCases().forEach(testCase -> testCase.setProblem(problem));
        }        
        return problemRepository.save(problem);
    }

    public Problem getProblemById(Long id) {
        return problemRepository.findById(id).orElse(null);
    }

    public List<ProblemSummaryDTO> getAllProblems() {
        return problemRepository.findAllSummaries();
    }

    public List<String> getTags(){
        // List<Tag> res = tagsRepository.findAll();
        // return res.stream().map((e)->e.getTag()).toList();
        return List.of("Strings","Maths","DP","Graph");
    }

    public Long getProblemCnt(){
        return problemRepository.count();
    }
    
}
