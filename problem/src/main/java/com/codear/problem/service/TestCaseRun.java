package com.codear.problem.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.codear.problem.dto.TestDTO;
import com.codear.problem.enums.SubmissionStatus;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TestCaseRun {

    private final CacheService cacheService;
    private final KafkaSender kafkaSender;

    private static final String KAFKA_TOPIC = "test-topic";

    public String processSubmittedCode(TestDTO testDTO) {
        String submissionId = UUID.randomUUID().toString();
        testDTO.setSubmissionId(submissionId);
        testDTO.setStatus(SubmissionStatus.IN_PROGRESS.toString());
        kafkaSender.sendMessage(testDTO,KAFKA_TOPIC);
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
