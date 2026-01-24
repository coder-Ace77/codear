package com.codear.engine.service;

import java.util.List;

import com.codear.engine.dto.CodeExecutionResult;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.TestDTO;
import com.codear.engine.entity.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.StopWatch;

@Slf4j
@Service
@AllArgsConstructor
public class SqsReceiver {

    private final ObjectMapper mapper;
    private final EngineService engineService;
    private final ProblemCrudService problemCrudService;
    private final CheckerService checkerService;
    private final SubmissionService submissionService;

    @SqsListener("codear-queue")
    public void listen(String message) {
        StopWatch stopWatch = new StopWatch("SqsReceiver.listen");
        try {
            stopWatch.start("log-start");
            log.info("Received submission message via SQS");
            stopWatch.stop();

            stopWatch.start("parse-message");
            Code code = mapper.readValue(message, Code.class);
            stopWatch.stop();

            stopWatch.start("fetch-test-cases");
            List<TestCase> testCases = problemCrudService.getAllTestCases(code.getProblemId());
            stopWatch.stop();

            stopWatch.start("prepare-inputs");
            List<String> inputs = testCases.stream().map(TestCase::getInput).toList();
            stopWatch.stop();

            stopWatch.start("fetch-constraints");
            ResourceConstraints resourceConstraints = problemCrudService.getPromblemConstraints(code.getProblemId());
            stopWatch.stop();

            stopWatch.start("execute-code");
            CodeExecutionResult result = engineService.runCode(
                    code.getCode(),
                    code.getLanguage(),
                    inputs,
                    resourceConstraints);
            stopWatch.stop();

            stopWatch.start("check-results");
            CheckerResponse checkerResponse = checkerService.check(result.getOutputs(), testCases);
            stopWatch.stop();

            stopWatch.start("update-submission");
            submissionService.updateSubmissionResult(code.getSubmissionId(), checkerResponse, result);
            stopWatch.stop();
        } catch (Exception e) {
            log.error("Error processing SQS submission: {}", e.getMessage());
            log.error("Raw message: {}", message);
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            log.info(stopWatch.prettyPrint());
        }
    }

    @SqsListener("codear-test")
    public void listenTest(String message) {
        StopWatch stopWatch = new StopWatch("SqsReceiver.listenTest");
        try {
            stopWatch.start("log-start");
            log.info("Received test-run message via SQS");
            stopWatch.stop();

            stopWatch.start("parse-message");
            TestDTO code = mapper.readValue(message, TestDTO.class);
            stopWatch.stop();

            stopWatch.start("fetch-constraints");
            ResourceConstraints resourceConstraints = problemCrudService.getPromblemConstraints(code.getProblemId());
            stopWatch.stop();

            stopWatch.start("execute-code");
            CodeExecutionResult result = engineService.runCode(
                    code.getCode(),
                    code.getLanguage(),
                    List.of(code.getInput()),
                    resourceConstraints);
            stopWatch.stop();

            stopWatch.start("update-result");
            String fullOutput = result.getOutputs().isEmpty() ? result.getLogs() : result.getOutputs().get(0);

            String finalOutput = fullOutput;
            String startMarker = "[TEST-OUTPUT-START]";
            String endMarker = "[TEST-OUTPUT-END]";

            int startIndex = fullOutput.indexOf(startMarker);
            int endIndex = fullOutput.indexOf(endMarker);

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                finalOutput = fullOutput.substring(startIndex + startMarker.length(), endIndex).trim();
            }
            submissionService.updateTestResult(code.getSubmissionId(), finalOutput);
            stopWatch.stop();
        } catch (Exception e) {
            log.error("Error processing SQS test request: {}", e.getMessage());
            log.error("Raw message: {}", message);
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            log.info(stopWatch.prettyPrint());
        }
    }
}