package com.codear.engine.service;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.TestDTO;
import com.codear.engine.enums.RunStatus;
import com.codear.engine.judge.JudgeReport;
import com.codear.engine.repository.SubmissionRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Saves what judging decided. */
@Slf4j
@Service
@AllArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final CacheService cacheService;

    /** True when the submission exists and has not been judged yet. */
    public boolean isInProgress(String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .map(submission -> submission.getStatus() == RunStatus.IN_PROGRESS)
                .orElse(false);
    }

    /**
     * Writes the verdict to the database and then flips the plain status flag the API also reads.
     *
     * @return false when the submission was already judged (a duplicate delivery), in which case nothing changed
     */
    public boolean recordVerdict(String submissionId, JudgeReport report) {
        RunStatus status = report.verdict().runStatus();
        int updated = submissionRepository.finishSubmission(
                submissionId,
                status,
                report.verdict().name(),
                report.failedTest(),
                report.message(),
                report.diagnostics(),
                report.totalTests(),
                report.passedTests(),
                report.maxTimeMs(),
                report.memoryLabel());
        if (updated == 0) {
            log.warn("Submission {} was already judged; keeping the first verdict", submissionId);
            return false;
        }
        cacheService.setValue(submissionId, status.toString());
        return true;
    }

    /** Stores the output of a person's own run where the API will find it. */
    public void updateTestResult(String submissionId, String result) {
        TestDTO testDTO = cacheService.getObjectValue(submissionId, TestDTO.class);
        if (testDTO == null) {
            log.warn("Test run {} expired before its result was ready", submissionId);
            return;
        }
        testDTO.setStatus(RunStatus.COMPLETED.toString());
        testDTO.setOutput(result);
        cacheService.setObjectValue(submissionId, testDTO);
    }
}
