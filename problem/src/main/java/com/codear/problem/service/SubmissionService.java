package com.codear.problem.service;

import com.codear.problem.dto.Submission;
import com.codear.problem.enums.SubmissionStatus;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SubmissionService {
    private final SubmissionReadService submissionReadService;
    private final CacheService cacheService;

    public Submission longPollingService(String submissionId) throws InterruptedException {
        int maxWaitTime = 10000;
        int baseInterval = 1000;
        int waited = 0;

        SubmissionStatus submission = SubmissionStatus.valueOf(cacheService.getValue(submissionId));
        try {
            while (submission == SubmissionStatus.IN_PROGRESS && waited < maxWaitTime) {
                Thread.sleep(baseInterval);
                waited += baseInterval;

                submission = SubmissionStatus.valueOf(cacheService.getValue(submissionId));                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Submission submission2 = submissionReadService.getSubmissionById(submissionId, waited);
        submission2.setStatus(submission);
        return submission2;
    }
}
