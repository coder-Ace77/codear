package com.codear.problem.controller;

import com.codear.problem.dto.ProblemSummaryDTO;
import com.codear.problem.entity.Problem;
import com.codear.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor 
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping("/addproblem")
    public ResponseEntity<Problem> createProblem(@RequestBody Problem problem) {
        Problem savedProblem = problemService.addProblem(problem);
        return new ResponseEntity<>(savedProblem, HttpStatus.CREATED);
    }

    @GetMapping("/problems")
    public ResponseEntity<List<ProblemSummaryDTO>> getAllProblems() {
        List<ProblemSummaryDTO> problems = problemService.getAllProblems();
        return ResponseEntity.ok(problems);
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
}