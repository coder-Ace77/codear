package com.codear.problem.controller;

import com.codear.problem.dto.ProblemSummaryDTO;
import com.codear.problem.dto.ProblemsMetaData;
import com.codear.problem.entity.Problem;
import com.codear.problem.service.ProblemService;
import com.codear.problem.service.SubmitService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.codear.problem.dto.Code;
import com.codear.problem.dto.ProblemSendDTO;

import org.springframework.web.bind.annotation.GetMapping;


@CrossOrigin("*")
@RestController
@RequiredArgsConstructor 
public class ProblemController {

    private final ProblemService problemService;
    private final SubmitService submitService;

    @PostMapping("/addproblem")
    public ResponseEntity<Problem> createProblem(@RequestBody Problem problem){
        Problem savedProblem = problemService.addProblem(problem);
        return new ResponseEntity<>(savedProblem, HttpStatus.CREATED);
    }

    @GetMapping("/problems")
    public ResponseEntity<List<ProblemSummaryDTO>> getAllProblems(){
        List<ProblemSummaryDTO> problems = problemService.getAllProblems();
        System.out.println(problems);
        return ResponseEntity.ok(problems);
    }

    @GetMapping("/problemCntAndTags")
    public ResponseEntity<ProblemsMetaData> getAllProblemsCount(){
        return ResponseEntity.ok(new ProblemsMetaData(){{
            setCount(problemService.getProblemCnt());
            setTags(problemService.getTags()); 
        }});
    }
    

    @GetMapping("/problem/{id}")
    public ResponseEntity<ProblemSendDTO> getProblemById(@PathVariable Long id) {
        ProblemSendDTO problem = problemService.getProblemById(id);
        return ResponseEntity.ok(problem); 
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleNotFound(RuntimeException ex) {
        System.out.println(ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    
    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> handleCodeSubmit(@RequestBody Code code) {
        String submissionId = submitService.processSubmittedCode(code);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Code submitted successfully");
        response.put("submissionId", submissionId);

        return ResponseEntity.ok(response);
    }

}