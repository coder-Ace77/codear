package com.codear.engine.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codear.engine.checker.ComparatorFactory;
import com.codear.engine.checker.OutputComparator;
import com.codear.engine.constants.JudgeDefaults;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.TestDTO;
import com.codear.engine.judge.CustomRunResult;
import com.codear.engine.judge.ExpectedTest;
import com.codear.engine.judge.FailureMessages;
import com.codear.engine.judge.JudgeReport;
import com.codear.engine.judge.JudgeRequest;
import com.codear.engine.judge.JudgeService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Takes work off the queue and hands it to the judge. The deciding is done elsewhere; this only wires things up. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqsReceiver {

    private final ObjectMapper mapper;
    private final ProblemCrudService problemCrudService;
    private final SubmissionService submissionService;
    private final ProgressReporterFactory progressFactory;
    private final JudgeService judgeService;

    @SqsListener(value = "codear-queue",
            maxConcurrentMessages = JudgeDefaults.LISTENER_MAX_CONCURRENT_MESSAGES,
            maxMessagesPerPoll = JudgeDefaults.LISTENER_MAX_MESSAGES_PER_POLL,
            messageVisibilitySeconds = JudgeDefaults.LISTENER_VISIBILITY_SECONDS)
    public void listen(String message) {
        Code code = null;
        ProgressReporter progress = null;
        try {
            code = mapper.readValue(message, Code.class);
            String submissionId = code.getSubmissionId();
            if (!submissionService.isInProgress(submissionId)) {
                log.info("Skipping {}: already judged or unknown", submissionId);
                return;
            }

            long startedAt = System.currentTimeMillis();
            progress = progressFactory.create(submissionId);
            progress.preparing();

            List<ExpectedTest> tests = loadTests(code.getProblemId());
            progress.totalKnown(tests.size());
            ResourceConstraints constraints = problemCrudService.getPromblemConstraints(code.getProblemId());

            JudgeReport report = judgeService.judge(
                    new JudgeRequest(code.getCode(), code.getLanguage(), tests, constraints, comparatorFor(constraints)),
                    progress);

            submissionService.recordVerdict(submissionId, report);
            progress.done();
            log.info("Judged {} as {} in {} ms", submissionId, report.verdict(), System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            log.error("Error processing SQS submission: {}", e.getMessage(), e);
            log.error("Raw message: {}", message);
            failSubmission(code, progress);
        }
    }

    @SqsListener(value = "codear-test",
            maxConcurrentMessages = JudgeDefaults.LISTENER_MAX_CONCURRENT_MESSAGES,
            maxMessagesPerPoll = JudgeDefaults.LISTENER_MAX_MESSAGES_PER_POLL,
            messageVisibilitySeconds = JudgeDefaults.LISTENER_VISIBILITY_SECONDS)
    public void listenTest(String message) {
        try {
            TestDTO request = mapper.readValue(message, TestDTO.class);
            ResourceConstraints constraints = problemCrudService.getPromblemConstraints(request.getProblemId());
            CustomRunResult result = judgeService.runCustom(
                    request.getCode(), request.getLanguage(), request.getInput(), constraints);
            submissionService.updateTestResult(request.getSubmissionId(), result.text());
        } catch (Exception e) {
            log.error("Error processing SQS test request: {}", e.getMessage(), e);
            log.error("Raw message: {}", message);
        }
    }

    private static OutputComparator comparatorFor(ResourceConstraints constraints) {
        return constraints == null
                ? ComparatorFactory.defaultComparator()
                : ComparatorFactory.forProblem(constraints.getChecker(), constraints.getCheckerTolerance());
    }

    private List<ExpectedTest> loadTests(Long problemId) {
        List<com.codear.engine.entity.TestCase> stored = problemCrudService.getAllTestCases(problemId);
        return java.util.stream.IntStream.range(0, stored.size())
                .mapToObj(i -> ExpectedTest.from(i, stored.get(i)))
                .toList();
    }

    /** Judging itself failed, so tell the person waiting instead of leaving the progress bar hanging. */
    private void failSubmission(Code code, ProgressReporter progress) {
        if (code == null || code.getSubmissionId() == null) {
            return;
        }
        try {
            submissionService.recordVerdict(code.getSubmissionId(),
                    JudgeReport.systemError(0, FailureMessages.judgeFailure()));
        } catch (Exception e) {
            log.error("Could not record the failure of submission {}: {}", code.getSubmissionId(), e.getMessage());
        }
        if (progress != null) {
            progress.error();
        }
    }
}
