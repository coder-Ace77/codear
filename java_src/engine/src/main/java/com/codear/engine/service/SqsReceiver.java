package com.codear.engine.service;

import org.springframework.stereotype.Service;

import com.codear.engine.constants.JudgeDefaults;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.TestDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsReceiver {

    private final ObjectMapper mapper;
    private final SubmissionProcessor submissionProcessor;

    @SqsListener(value = "codear-queue",
            maxConcurrentMessages = JudgeDefaults.LISTENER_MAX_CONCURRENT_MESSAGES,
            maxMessagesPerPoll = JudgeDefaults.LISTENER_MAX_MESSAGES_PER_POLL,
            messageVisibilitySeconds = JudgeDefaults.LISTENER_VISIBILITY_SECONDS)
    public void listen(String message) {
        try {
            submissionProcessor.processSubmission(mapper.readValue(message, Code.class));
        } catch (JsonProcessingException e) {
            log.error("Unreadable submission message: {}", message, e);
        }
    }

    @SqsListener(value = "codear-test",
            maxConcurrentMessages = JudgeDefaults.LISTENER_MAX_CONCURRENT_MESSAGES,
            maxMessagesPerPoll = JudgeDefaults.LISTENER_MAX_MESSAGES_PER_POLL,
            messageVisibilitySeconds = JudgeDefaults.LISTENER_VISIBILITY_SECONDS)
    public void listenTest(String message) {
        try {
            submissionProcessor.processTestRun(mapper.readValue(message, TestDTO.class));
        } catch (JsonProcessingException e) {
            log.error("Unreadable test message: {}", message, e);
        }
    }
}
