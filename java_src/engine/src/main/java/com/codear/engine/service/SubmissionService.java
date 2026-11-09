package com.codear.engine.service;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.RunRequest;
import com.codear.engine.dto.judge.JudgeReport;
import com.codear.engine.enums.RunStatus;
import com.codear.engine.repository.SubmissionRepository;
import com.codear.engine.util.CacheService;
import com.codear.engine.util.judge.FailureMessages;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final CacheService cacheService;

    public boolean isInProgress(String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .map(submission -> submission.getStatus() == RunStatus.IN_PROGRESS)
                .orElse(false);
    }

    public boolean recordVerdict(String submissionId, JudgeReport report) {
        RunStatus status = report.getVerdict().runStatus();
        int updated = submissionRepository.finishSubmission(
                submissionId,
                status,
                report.getVerdict().name(),
                report.getFailedTest(),
                report.getMessage(),
                report.getDiagnostics(),
                report.getTotalTests(),
                report.getPassedTests(),
                report.getMaxTimeMs(),
                report.memoryLabel());
        if(updated == 0){
            return false;
        }
        cacheService.set(submissionId, status.toString());
        return true;
    }

    public void recordJudgeFailure(String submissionId) {
        if (submissionId == null) {
            return;
        }
        try {
            recordVerdict(submissionId, JudgeReport.systemError(0, FailureMessages.judgeFailure()));
        } catch (Exception e) {
            log.error("Could not record the failure of submission {}: {}", submissionId, e.getMessage());
        }
    }

    public void saveRunResult(String runId, String result) {
        RunRequest run = cacheService.get(runId, RunRequest.class);
        if (run == null) {
            log.warn("Run {} expired before its result was ready", runId);
            return;
        }
        run.setStatus(RunStatus.COMPLETED.toString());
        run.setOutput(result);
        cacheService.set(runId, run);
    }
}
