package com.codear.problem.controller;

import com.codear.problem.dto.Submission;
import com.codear.problem.dto.TestDTO;
import com.codear.problem.service.SubmissionReadService;
import com.codear.problem.service.SubmissionService;
import com.codear.problem.service.TestCaseRun;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.*;


@RestController
@RequestMapping("api/v1/problem/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final TestCaseRun testCaseRun;
    private final SubmissionReadService submissionReadService;

    @GetMapping("/{submissionId}")
    public Submission longPollStatus(@PathVariable String submissionId) throws InterruptedException {
        return submissionService.longPollingService(submissionId);
    }

    @GetMapping("/test/{submissionId}")
    public TestDTO testPolling(@PathVariable String submissionId) throws InterruptedException{
        return testCaseRun.longPollingService(submissionId);
    }

    @GetMapping("/subuser/{problemId}")
    public ResponseEntity<List<Submission>> getMethodName(@PathVariable String problemId,@RequestHeader(name="authorization") String authString){
        return ResponseEntity.ok(submissionReadService.getSubmissionByIdAndProblem(authString,Long.valueOf(problemId)));
    }
    
    
}
