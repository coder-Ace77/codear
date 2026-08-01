package com.codear.problem.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.codear.problem.dto.Code;
import com.codear.problem.dto.Submission;
import com.codear.problem.enums.SubmissionStatus;
import com.codear.problem.repository.SubmissionRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SubmitService {

    private final SqsSender sqsSender;
    private final SubmissionRepository submissionRepository;
    private final CacheService cacheService;

    public String processSubmittedCode(Code code) {
        String submissionId = UUID.randomUUID().toString();
        code.setSubmissionId(submissionId);

        cacheService.setValue(submissionId,SubmissionStatus.IN_PROGRESS.toString());

        sqsSender.sendMessage(code);

        Submission submission = Submission.builder()
                .submissionId(submissionId)
                .problemId(code.getProblemId())
                .language(code.getLanguage())
                .code(code.getCode())
                .status(SubmissionStatus.IN_PROGRESS)
                .submittedAt(LocalDateTime.now())
                .userId(code.getUserId())
                .build();

        submissionRepository.save(submission);

        return submissionId;
    }
}
