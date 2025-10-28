package com.codear.problem.controller;

import com.codear.problem.SubmissionService;
import com.codear.problem.dto.Submission;
import com.codear.problem.enums.SubmissionStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @GetMapping("/{submissionId}")
    public Submission longPollStatus(@PathVariable String submissionId) throws InterruptedException {
        int maxWaitTime = 15000;  // 15 seconds
        int baseInterval = 100;  // 100ms
        int waited = 0;
        int ct = 0;

        Submission submission = submissionService.getSubmissionById(submissionId);

        while (submission.getStatus() == SubmissionStatus.IN_PROGRESS && waited < maxWaitTime) {
            int currentInterval = Math.min(baseInterval * (1 << (ct)), 2000); 
            // ensure interval caps at 2000ms to avoid too long waits

            Thread.sleep(currentInterval);
            waited += currentInterval;

            submission = submissionService.getSubmissionById(submissionId);
            ct++;
        }

        System.out.println("submission" + submission);

        return submission;
    }

    @GetMapping("/all")
    public List<Submission> getSubmissionsByProblemId(@RequestParam Long problemId) {
        return submissionService.getSubmissionsByProblemId(problemId);
    }
}
