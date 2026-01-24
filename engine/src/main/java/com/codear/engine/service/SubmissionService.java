package com.codear.engine.service;

import java.time.LocalDateTime;

import com.codear.engine.dto.CodeExecutionResult;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.TestDTO;
import com.codear.engine.entity.Submission;
import com.codear.engine.enums.RunStatus;
import com.codear.engine.repository.SubmissionRepository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final CacheService cacheService;

    public Submission getSubmissionObject(Code code, CheckerResponse checkerResponse) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("SubmissionService.getSubmissionObject");
        try {
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
        } finally {
            stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSubmissionResult(String submissionId, CheckerResponse checkerResponse,
            CodeExecutionResult executionResult) {
        StopWatch stopWatch = new StopWatch("SubmissionService.updateSubmissionResult");
        try {
            stopWatch.start("update-db-direct");
            submissionRepository.updateSubmissionResult(
                    submissionId,
                    checkerResponse.getStatus() != null ? checkerResponse.getStatus() : RunStatus.FAILED,
                    checkerResponse.getMsg(),
                    executionResult.getLogs(),
                    checkerResponse.getTotalTests(),
                    checkerResponse.getPassedTests(),
                    executionResult.getCpuTimeMs(),
                    executionResult.getMemoryUsedPk());
            stopWatch.stop();
            stopWatch.start("update-cache");
            cacheService.setValue(submissionId, checkerResponse.getStatus().toString());
            stopWatch.stop();
        } finally {
            if (stopWatch.isRunning())
                stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

    public void updateTestResult(String submissionId, String result) {
        StopWatch stopWatch = new StopWatch("SubmissionService.updateTestResult");
        try {
            stopWatch.start("fetch-cache");
            TestDTO testDTO = cacheService.getObjectValue(submissionId, TestDTO.class);
            stopWatch.stop();

            stopWatch.start("update-dto");
            testDTO.setStatus(RunStatus.COMPLETED.toString());
            testDTO.setOutput(result);
            stopWatch.stop();

            stopWatch.start("save-cache");
            cacheService.setObjectValue(submissionId, testDTO);
            stopWatch.stop();
        } finally {
            if (stopWatch.isRunning())
                stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

}
