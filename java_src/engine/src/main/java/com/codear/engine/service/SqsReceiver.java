package com.codear.engine.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codear.engine.checker.ComparatorFactory;
import com.codear.engine.constants.JudgeDefaults;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.TestDTO;
import com.codear.engine.dto.judge.CustomRunResult;
import com.codear.engine.dto.judge.ExpectedTest;
import com.codear.engine.dto.judge.JudgeReport;
import com.codear.engine.dto.judge.JudgeRequest;
import com.codear.engine.judge.JudgeService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsReceiver {

    private final ObjectMapper mapper;
    private final ProblemCrudService problemCrudService;
    private final SubmissionService submissionService;
    private final JudgeService judgeService;

    @SqsListener(value = "codear-queue",
            maxConcurrentMessages = JudgeDefaults.LISTENER_MAX_CONCURRENT_MESSAGES,
            maxMessagesPerPoll = JudgeDefaults.LISTENER_MAX_MESSAGES_PER_POLL,
            messageVisibilitySeconds = JudgeDefaults.LISTENER_VISIBILITY_SECONDS)
    public void listen(String message) {
        Code code = null;
        try {
            code = mapper.readValue(message, Code.class);
            String submissionId = code.getSubmissionId();
            if (!submissionService.isInProgress(submissionId)) {
                log.info("Skipping {}: already judged or unknown", submissionId);
                return;
            }

            List<ExpectedTest> tests = ExpectedTest.fromAll(problemCrudService.getAllTestCases(code.getProblemId()));
            ResourceConstraints constraints = problemCrudService.getPromblemConstraints(code.getProblemId());

            JudgeReport report = judgeService.judge(new JudgeRequest(code.getCode(), code.getLanguage(), tests,
                    constraints, ComparatorFactory.forConstraints(constraints)));

            submissionService.recordVerdict(submissionId, report);
        } catch (Exception e) {
            log.error("Error processing SQS submission: {}", e.getMessage(), e);
            log.error("Raw message: {}", message);
            if (code != null){
                submissionService.recordJudgeFailure(code.getSubmissionId());
            }
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
            submissionService.updateTestResult(request.getSubmissionId(), result.getText());
        } catch (Exception e) {
            log.error("Error processing SQS test request: {}", e.getMessage(), e);
            log.error("Raw message: {}", message);
        }
    }
}
