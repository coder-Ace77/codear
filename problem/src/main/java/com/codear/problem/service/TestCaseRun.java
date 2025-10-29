package com.codear.problem.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.codear.problem.dto.Submission;
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
        System.out.println("[KAFKA] Cache service updating");
        kafkaSender.sendMessage(testDTO,KAFKA_TOPIC);
        System.out.println("[KAFKA] Send for the long poll");
        testDTO.setSubmissionId(submissionId);
        System.out.println("[CACHE] updating cahe for test");
        cacheService.setObjectValue(submissionId,testDTO);
        System.out.println("[CACHE] updated cache");
        return submissionId;
    }

    public TestDTO longPollingService(String submissionId) throws InterruptedException {

        int maxWaitTime = 1000;
        int baseInterval = 200;
        int waited = 0;

        System.out.println("Entered long polling service for " + submissionId);

        TestDTO submission = cacheService.getObjectValue(submissionId,TestDTO.class);

        long startTime = System.currentTimeMillis();
        System.out.println("⏳ Entered polling loop for " + submissionId);

        try {
            while (submission.getStatus() == "IN_PROGRESSS" && waited < maxWaitTime) {
                Thread.sleep(baseInterval);
                waited += baseInterval;
                submission = cacheService.getObjectValue(submissionId,TestDTO.class);
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

        return submission;
    }
}
