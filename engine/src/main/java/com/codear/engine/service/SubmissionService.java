package com.codear.engine.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.TestDTO;
import com.codear.engine.entity.Submission;
import com.codear.engine.enums.RunStatus;
import com.codear.engine.repository.SubmissionRepository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final CacheService cacheService;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSubmissionResult(String submissionId, CheckerResponse checkerResponse) {
        
        System.out.println("Updating the submission id"+submissionId);
        Submission submission = submissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        submission.setStatus(checkerResponse.getStatus() != null ? checkerResponse.getStatus() : RunStatus.FAILED);
        submission.setResult(checkerResponse.getMsg());
        submission.setTotalTests(checkerResponse.getTotalTests());
        submission.setPassedTests(checkerResponse.getPassedTests());
        submission.setTimeTakenMs(submission.getTimeTakenMs() != null ? submission.getTimeTakenMs() : 0L);
        submission.setMemoryUsed(submission.getMemoryUsed() != null ? submission.getMemoryUsed() : "0MB");

        Submission saved = submissionRepository.save(submission);
        
        System.out.println("Updated the submission id"+submissionId);

        cacheService.setValue(submissionId,checkerResponse.getStatus().toString());

        System.out.println("✅ Submission updated successfully.");

        System.out.println("NEW STATUS UPDATED::"+cacheService.getValue(submissionId));

    }

    public void updateTestResult(String submissionId,String result){
        TestDTO testDTO = cacheService.getObjectValue(submissionId, TestDTO.class);
        testDTO.setStatus(RunStatus.COMPLETED.toString());
        testDTO.setOutput(result);
        cacheService.setObjectValue(submissionId,testDTO);
        System.out.println("[ENGINE] Updated submission "+testDTO);
    }

}
