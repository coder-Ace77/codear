package com.codear.problem.controller;

import com.codear.problem.SubmissionService;
import com.codear.problem.dto.Submission;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@RestController
@RequestMapping("api/v1/problem/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @GetMapping("/{submissionId}")
    public Submission longPollStatus(@PathVariable String submissionId) throws InterruptedException {
        System.out.println("Entreted submission controller");
        return submissionService.longPollingService(submissionId);
    }

    // @GetMapping("/all")
    // public List<Submission> getSubmissionsByProblemId(@RequestParam Long problemId) {
    //     return submissionService.getSubmissionsByProblemId(problemId);
    // }
}
