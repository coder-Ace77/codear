package com.codear.engine.service;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.entity.TestCase;
import com.codear.engine.repository.ProblemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class KafkaReceiver {

    private final ObjectMapper mapper; 
    private final EngineService engineService;
    private final ProblemCrudService problemCrudService;
    private final CheckerService checkerService;
    private final ProblemRepository problemRepository;
    private final SubmissionService submissionService;

    @KafkaListener(topics = "code-submit", groupId = "codear")
    public void listen(String message) {
        try {
            // 1️⃣ Parse Kafka message
            Code code = mapper.readValue(message, Code.class);

            // 2️⃣ Fetch test cases & constraints
            List<TestCase> testCases = problemCrudService.getAllTestCases(code.getProblemId());
            List<String> inputs = testCases.stream().map(TestCase::getInput).toList();

            ResourceConstraints resourceConstraints = problemRepository.findConstraintsById(code.getProblemId())
                    .orElseThrow(() -> new IllegalArgumentException("Problem not found with id: " + code.getProblemId()));

            // 3️⃣ Run code
            List<String> result = engineService.runCode(
                    code.getCode(),
                    code.getLanguage(),
                    inputs,
                    resourceConstraints
            );

            // 4️⃣ Check results
            CheckerResponse checkerResponse = checkerService.check(result, testCases);
            System.out.println("Checker Response: " + checkerResponse);

            // 5️⃣ Update submission in DB
            submissionService.updateSubmissionResult(code.getSubmissionId(), checkerResponse);

        } catch (Exception e) {
            System.err.println("Error parsing JSON or executing code: " + e.getMessage());
            System.err.println("Raw message: " + message);
        }
    }
}
