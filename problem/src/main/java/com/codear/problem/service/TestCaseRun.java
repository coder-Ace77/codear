package com.codear.problem.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.codear.problem.dto.TestDTO;
import com.codear.problem.enums.SubmissionStatus;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TestCaseRun {

    private final CacheService cacheService;
    private final SqsSender sqsSender;
    
    private static final String SQS_TEST_QUEUE = "https://sqs.ap-south-1.amazonaws.com/829108230523/codear-test";


    public String processSubmittedCode(TestDTO testDTO) {
        String submissionId = UUID.randomUUID().toString();
        testDTO.setSubmissionId(submissionId);
        testDTO.setStatus(SubmissionStatus.IN_PROGRESS.toString());
        sqsSender.sendMessage(testDTO,SQS_TEST_QUEUE);
        testDTO.setSubmissionId(submissionId);
        cacheService.setObjectValue(submissionId,testDTO);
        return submissionId;
    }

    public TestDTO longPollingService(String submissionId) throws InterruptedException {

        int maxWaitTime = 1000;
        int baseInterval = 200;
        int waited = 0;

        TestDTO submission = cacheService.getObjectValue(submissionId,TestDTO.class);
        try {
            while (submission.getStatus() == "IN_PROGRESSS" && waited < maxWaitTime) {
                Thread.sleep(baseInterval);
                waited += baseInterval;
                submission = cacheService.getObjectValue(submissionId,TestDTO.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return submission;
    }
}
