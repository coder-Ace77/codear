package com.codear.engine.service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.entity.Submission;
import com.codear.engine.entity.TestCase;
import com.codear.engine.repository.ProblemRepository;
import com.codear.engine.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor; 

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor 
public class KafkaReceiver {
    
    private final ObjectMapper mapper; 
    private final EngineService engineService;
    private final ProblemCrudService problemCrudService;
    private final CheckerService checkerService;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionService submissionService;

    @KafkaListener(topics = "code-submit", groupId = "codear")
    public void listen(String message) {
        try {

            Code code = mapper.readValue(message, Code.class);             

            List<TestCase> testCases = problemCrudService.getAllTestCases(code.getProblemId());
            
            List<String> inputs = testCases.stream().map((e)->e.getInput()).toList();

            ResourceConstraints resourceConstraints = problemRepository.findConstraintsById(code.getProblemId())
                .orElseThrow(() -> new IllegalArgumentException("Problem not found with id: " + code.getProblemId()));

            List<String> result = engineService.runCode(code.getCode(), code.getLanguage(),inputs, resourceConstraints);
            
            CheckerResponse checkerResponse = checkerService.check(result, testCases);

            System.out.println("Checker Response: " + checkerResponse);

            Submission submission = submissionService.getSubmissionObject(code, checkerResponse);
            submissionRepository.save(submission);

        } catch (Exception e) {
            System.err.println("Error parsing JSON or executing code: " + e.getMessage());
            System.err.println("Raw message: " + message);
        }
    }
}