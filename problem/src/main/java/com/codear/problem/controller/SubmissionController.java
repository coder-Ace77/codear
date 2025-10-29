package com.codear.problem.controller;

import com.codear.problem.dto.Submission;
import com.codear.problem.dto.TestDTO;
import com.codear.problem.service.SubmissionService;
import com.codear.problem.service.TestCaseRun;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("api/v1/problem/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final TestCaseRun testCaseRun;

    @GetMapping("/{submissionId}")
    public Submission longPollStatus(@PathVariable String submissionId) throws InterruptedException {
        System.out.println("Entreted submission controller");
        return submissionService.longPollingService(submissionId);
    }

    @GetMapping("/test/{submissionId}")
    public TestDTO testPolling(@PathVariable String submissionId) throws InterruptedException{
        return testCaseRun.longPollingService(submissionId);
    }
    
}
