package com.codear.engine.service;

import java.util.List;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Service;

import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.TestDTO;
import com.codear.engine.entity.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        try {
            System.out.println("Received submission message via SQS");
            
            Code code = mapper.readValue(message, Code.class);
            List<TestCase> testCases = problemCrudService.getAllTestCases(code.getProblemId());
            List<String> inputs = testCases.stream().map(TestCase::getInput).toList();
            ResourceConstraints resourceConstraints = problemCrudService.getPromblemConstraints(code.getProblemId());
            
            List<String> result = engineService.runCode(
                    code.getCode(),
                    code.getLanguage(),
                    inputs,
                    resourceConstraints
            );
            
            submissionService.updateSubmissionResult(code.getSubmissionId(), checkerService.check(result, testCases));
        } catch (Exception e) {
            log.error("Error processing SQS submission: {}", e.getMessage());
            log.error("Raw message: {}", message);
        }
    }

    @SqsListener("codear-test")
    public void listenTest(String message) {
        try {
            System.out.println("Received test-run message via SQS");

            TestDTO code = mapper.readValue(message, TestDTO.class);
            ResourceConstraints resourceConstraints = problemCrudService.getPromblemConstraints(code.getProblemId());
            
            List<String> result = engineService.runCode(
                    code.getCode(),
                    code.getLanguage(),
                    List.of(code.getInput()),
                    resourceConstraints
            );
            
            submissionService.updateTestResult(code.getSubmissionId(), result.get(0));
        } catch (Exception e) {
            log.error("Error processing SQS test request: {}", e.getMessage());
            log.error("Raw message: {}", message);
        }
    }
}