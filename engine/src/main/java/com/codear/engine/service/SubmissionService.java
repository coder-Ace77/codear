package com.codear.engine.service;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.dto.Code;
import com.codear.engine.entity.Submission;

@Service
public class SubmissionService {
    public Submission getSubmissionObject(Code code,CheckerResponse checkerResponse) {

        Submission submission = new Submission();
        submission.setProblemId(code.getProblemId());
        submission.setCode(code.getCode());
        submission.setLanguage(code.getLanguage());
        submission.setUserId(code.getUserId());
        submission.setStatus(checkerResponse.getStatus());
        submission.setResult(checkerResponse.getMsg());
        submission.setTotalTests(checkerResponse.getTotalTests());
        submission.setPassedTests(checkerResponse.getPassedTests());
        submission.setSubmittedAt(code.getSubmittedAt());
        submission.setTimeTakenMs(0L);
        submission.setMemoryUsed("0MB");

        return submission;
    }
}
