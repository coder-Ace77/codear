package com.codear.engine.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.dto.Code;
import com.codear.engine.entity.Submission;
import com.codear.engine.enums.RunStatus;
import com.codear.engine.repository.SubmissionRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    /**
     * Create a Submission object from Code and CheckerResponse
     */
    public Submission getSubmissionObject(Code code, CheckerResponse checkerResponse) {
        Submission submission = new Submission();
        submission.setSubmissionId(code.getSubmissionId());
        submission.setProblemId(code.getProblemId());
        submission.setUserId(code.getUserId());
        submission.setCode(code.getCode());
        submission.setLanguage(code.getLanguage());
        submission.setStatus(checkerResponse.getStatus() != null ? checkerResponse.getStatus() : RunStatus.FAILED);
        submission.setResult(checkerResponse.getMsg());   
        submission.setTotalTests(checkerResponse.getTotalTests());
        submission.setPassedTests(checkerResponse.getPassedTests());
        submission.setSubmittedAt(code.getSubmittedAt() != null ? code.getSubmittedAt() : LocalDateTime.now());
        submission.setTimeTakenMs(0L);    
        submission.setMemoryUsed("0MB");    
        return submission;
    }

    /**
     * Update an existing submission in DB after code execution
     */
    public Submission updateSubmissionResult(String submissionId, CheckerResponse checkerResponse) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        submission.setStatus(checkerResponse.getStatus() != null ? checkerResponse.getStatus() : RunStatus.FAILED);
        submission.setResult(checkerResponse.getMsg());
        submission.setTotalTests(checkerResponse.getTotalTests());
        submission.setPassedTests(checkerResponse.getPassedTests());

        submission.setTimeTakenMs(submission.getTimeTakenMs() != null ? submission.getTimeTakenMs() : 0L);
        submission.setMemoryUsed(submission.getMemoryUsed() != null ? submission.getMemoryUsed() : "0MB");

        return submissionRepository.save(submission);
    }
}
