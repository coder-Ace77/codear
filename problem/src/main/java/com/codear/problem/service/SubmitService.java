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

    private final KafkaSender kafkaSender;
    private final SubmissionRepository submissionRepository;

    public String processSubmittedCode(Code code) {
        String submissionId = UUID.randomUUID().toString();
        code.setSubmissionId(submissionId);

        kafkaSender.sendMessage(code);

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
