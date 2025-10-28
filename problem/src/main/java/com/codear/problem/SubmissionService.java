package com.codear.problem;

import com.codear.problem.dto.Submission;
import com.codear.problem.enums.SubmissionStatus;
import com.codear.problem.service.CacheService;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SubmissionService {
    private final SubmissionReadService submissionReadService;
    private final CacheService cacheService;

    public Submission longPollingService(String submissionId) throws InterruptedException {
        int maxWaitTime = 10000;
        int baseInterval = 100;
        int waited = 0;

        System.out.println("Entered long polling service for " + submissionId);

        SubmissionStatus submission = SubmissionStatus.valueOf(cacheService.getValue(submissionId));

        long startTime = System.currentTimeMillis();
        System.out.println("⏳ Entered polling loop for " + submissionId);

        try {
            while (submission == SubmissionStatus.IN_PROGRESS && waited < maxWaitTime) {
                Thread.sleep(baseInterval);
                waited += baseInterval;

                submission = SubmissionStatus.valueOf(cacheService.getValue(submissionId));                
                System.out.println("Looping... waited=" + waited + " status=" + submission);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Exception in polling loop: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Exited polling loop for " + submissionId);
        System.out.println("Total time spent: " + duration + " ms");
        System.out.println("Final submission status: " + submission);

        Submission submission2 = submissionReadService.getSubmissionById(submissionId, waited);
        submission2.setStatus(submission);
        return submission2;
    }

}
