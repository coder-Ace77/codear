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

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.codear.problem.dto.Code;
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
    public ResponseEntity<Problem> getProblemById(@PathVariable Long id) {
        Problem problem = problemService.getProblemById(id);
        return ResponseEntity.ok(problem); 
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleNotFound(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @PostMapping("/submit")
    public ResponseEntity<String> handleCodeSubmit(@RequestBody Code code){
        System.out.println("Received code submission: " + code);
        submitService.processSubmittedCode(code);
        return ResponseEntity.ok("Code submitted successfully");
    }
    
}