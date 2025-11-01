package com.codear.engine.service;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.TestDTO;
import com.codear.engine.entity.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class KafkaReceiver {

    private final ObjectMapper mapper; 
    private final EngineService engineService;
    private final ProblemCrudService problemCrudService;
    private final CheckerService checkerService;
    private final SubmissionService submissionService;

    @KafkaListener(topics = "code-submit", groupId = "codear")
    public void listen(String message) {
        try {
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
            submissionService.updateSubmissionResult(code.getSubmissionId(),checkerService.check(result, testCases));
        } catch (Exception e) {
            System.err.println("Error parsing JSON or executing code: " + e.getMessage());
            System.err.println("Raw message: " + message);
        }
    }

    @KafkaListener(topics = "test-topic", groupId = "codear")
    public void listenTest(String message){
        try {
            TestDTO code = mapper.readValue(message, TestDTO.class);
            ResourceConstraints resourceConstraints = problemCrudService.getPromblemConstraints(code.getProblemId());
            List<String> result = engineService.runCode(
                    code.getCode(),
                    code.getLanguage(),
                    List.of(code.getInput()),
                    resourceConstraints
            );
            submissionService.updateTestResult(code.getSubmissionId(),result.get(0));
        } catch (Exception e) {
            System.err.println("Error parsing JSON or executing code: " + e.getMessage());
            System.err.println("Raw message: " + message);
        }
    }
}
