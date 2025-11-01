package com.codear.problem.controller;

import com.codear.problem.dto.ProblemSummaryDTO;
import com.codear.problem.dto.ProblemsMetaData;
import com.codear.problem.dto.TestDTO;
import com.codear.problem.entity.Problem;
import com.codear.problem.service.ProblemService;
import com.codear.problem.service.SubmitService;
import com.codear.problem.service.TestCaseRun;
import com.codear.problem.service.UserServiceJWT;

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

@RestController
@RequestMapping("/api/v1/problem")
@RequiredArgsConstructor 
public class ProblemController {

    private final ProblemService problemService;
    private final SubmitService submitService;
    private final TestCaseRun testCaseRun;
    private final UserServiceJWT userServiceJWT;

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
    public ResponseEntity<Map<String, String>> handleCodeSubmit(@RequestHeader(name="Authorization") String authString,@RequestBody Code code){
        Long userId = userServiceJWT.getUserIdByToken(authString);
        code.setUserId(userId);
        System.out.println(code);
        String submissionId = submitService.processSubmittedCode(code);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Code submitted successfully");
        response.put("submissionId", submissionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public Map<String, Object> getProblems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        System.out.println("get the request at /search");

        List<ProblemSummaryDTO> problems = problemService.searchProblems(search, difficulty, tags, page, size);
        long totalCount = problemService.countFilteredProblems(search, difficulty, tags);

        System.out.println("result from service " + problems);

        Map<String, Object> response = new HashMap<>();
        response.put("content", problems);
        response.put("totalCount", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / size));

        return response;
    }


    @GetMapping("/health-check")
    public ResponseEntity<String> HealthCheckProblem() {
        System.out.println("health check");
        return ResponseEntity.ok("health is running");
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> postMethodName(@RequestBody TestDTO testDTO){
        String submissionId = testCaseRun.processSubmittedCode(testDTO);
        Map<String,String> response = new HashMap<>();
        response.put("message","Test in queue");
        response.put("submissionId",submissionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ProblemSummaryDTO>> getMethodName(@RequestHeader(name="Authorization") String authString){
        Long userId = userServiceJWT.getUserIdByToken(authString);
        return ResponseEntity.ok(problemService.getProblemSummaryRecent(userId));
    }   
}