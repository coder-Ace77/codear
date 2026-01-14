package com.codear.engine.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.TestDTO;
import com.codear.engine.entity.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class LambdaHandler implements RequestHandler<SQSEvent, Void> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final EngineLambda engineLambda = new EngineLambda();
    
    private ProblemCrudService problemCrudService;
    private CheckerService checkerService;
    private SubmissionService submissionService;

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                String body = msg.getBody();
                
                if (body.contains("submissionId") && body.contains("code")) {
                    processSubmission(body);
                } else {
                    processTest(body);
                }
            } catch (Exception e) {
                log.error("Failed to process message: {}", e.getMessage());
            }
        }
        return null;
    }

    private void processSubmission(String message) throws Exception {
        Code code = mapper.readValue(message, Code.class);
        List<TestCase> testCases = problemCrudService.getAllTestCases(code.getProblemId());
        List<String> inputs = testCases.stream().map(TestCase::getInput).toList();
        ResourceConstraints constraints = problemCrudService.getPromblemConstraints(code.getProblemId());
        
        List<String> results = engineLambda.runCode(code.getCode(), code.getLanguage(), inputs, constraints);
        submissionService.updateSubmissionResult(code.getSubmissionId(), checkerService.check(results, testCases));
    }

    private void processTest(String message) throws Exception {
        TestDTO test = mapper.readValue(message, TestDTO.class);
        ResourceConstraints constraints = problemCrudService.getPromblemConstraints(test.getProblemId());
        
        List<String> results = engineLambda.runCode(test.getCode(), test.getLanguage(), List.of(test.getInput()), constraints);
        submissionService.updateTestResult(test.getSubmissionId(), results.get(0));
    }
}