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
        int maxWaitTime = 15000;  // 30 seconds
        int pollInterval = 2000;  // 2 seconds
        int waited = 0;

        Submission submission = submissionService.getSubmissionById(submissionId);

        while (submission.getStatus() == SubmissionStatus.IN_PROGRESS && waited < maxWaitTime) {

            Thread.sleep(pollInterval);
            waited += pollInterval;

            submission = submissionService.getSubmissionById(submissionId);
        }

        return submission;
    }

    @GetMapping("/all")
    public List<Submission> getSubmissionsByProblemId(@RequestParam Long problemId) {
        return submissionService.getSubmissionsByProblemId(problemId);
    }
}
