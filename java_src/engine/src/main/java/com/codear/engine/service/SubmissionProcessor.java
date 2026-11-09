package com.codear.engine.service;

import static com.codear.engine.constants.JudgeDefaults.MAX_SOURCE_CHARS;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codear.engine.checker.ComparatorFactory;
import com.codear.engine.dto.Code;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.RunRequest;
import com.codear.engine.dto.judge.RunReport;
import com.codear.engine.dto.judge.ExpectedTest;
import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.dto.judge.JudgeReport;
import com.codear.engine.dto.sandbox.SandboxResult;
import com.codear.engine.enums.Language;
import com.codear.engine.exception.SandboxException;
import com.codear.engine.judge.JudgeService;
import com.codear.engine.sandbox.SandboxRunner;
import com.codear.engine.util.judge.FailureMessages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionProcessor {

    private final ProblemCrudService problemCrudService;
    private final SubmissionService submissionService;
    private final SandboxRunner sandbox;
    private final JudgeService judgeService;

    public void processSubmission(Code code) {
        String submissionId = code.getSubmissionId();
        try {
            if (!submissionService.isInProgress(submissionId)) {
                log.info("Skipping {}: already judged or unknown", submissionId);
                return;
            }
            Long problemId = code.getProblemId();
            List<ExpectedTest> tests = ExpectedTest.fromAll(problemCrudService.getAllTestCases(problemId));
            ResourceConstraints constraints = problemCrudService.getPromblemConstraints(problemId);

            JudgeReport report = run(code.getCode(), code.getLanguage(), tests, constraints);

            submissionService.recordVerdict(submissionId, report);
        } catch (RuntimeException e) {
            log.error("Error judging submission {}: {}", submissionId, e.getMessage(), e);
            submissionService.recordJudgeFailure(submissionId);
        }
    }

    public void processRun(RunRequest request) {
        try {
            ResourceConstraints constraints = problemCrudService.getPromblemConstraints(request.getProblemId());
            RunReport result = run(
                    request.getCode(), request.getLanguage(), request.getInput(), constraints);
            submissionService.saveRunResult(request.getRunId(), result.getText());
        } catch (RuntimeException e) {
            log.error("Error on run {}: {}", request.getRunId(), e.getMessage(), e);
        }
    }

    // runs for the submission
    public JudgeReport run(String code, String language, List<ExpectedTest> tests,
            ResourceConstraints constraints) {
        int total = tests.size();
        String rejection = validateCodeAndLanguage(code, language);
        if (rejection != null) {
            return JudgeReport.compileError(total, rejection, "");
        }

        JudgeLimits limits = JudgeLimits.forProblem(constraints, tests);
        List<String> inputs = tests.stream().map(ExpectedTest::getInput).toList();

        SandboxResult result;
        try {
            result = sandbox.run(Language.from(language), code, inputs, limits);
        } catch (SandboxException e) {
            log.error("Sandbox failed: {}", e.getMessage(), e);
            return JudgeReport.systemError(total, FailureMessages.judgeFailure());
        }

        return judgeService.judge(result, tests, limits, ComparatorFactory.getCheckerFromConstraints(constraints));
    }

    // runs when user wants to run against custom test not submit code. 
    public RunReport run(String code, String language, String input,
            ResourceConstraints constraints) {
        String rejection = validateCodeAndLanguage(code, language);
        if (rejection != null) {
            return new RunReport(rejection, false);
        }

        JudgeLimits limits = JudgeLimits.forCustomRun(constraints);
        List<String> inputs = List.of(input == null ? "" : input);

        SandboxResult result;
        try {
            result = sandbox.run(Language.from(language), code, inputs, limits);
        } catch (SandboxException e) {
            log.error("Sandbox failed on a custom run: {}", e.getMessage(), e);
            return new RunReport(FailureMessages.judgeFailure(), false);
        }

        return judgeService.judgeCustomRun(result, limits);
    }

    private static String validateCodeAndLanguage(String code, String language) {
        if (code == null || code.isBlank()) {
            return FailureMessages.emptySource();
        }
        if (code.length() > MAX_SOURCE_CHARS) {
            return FailureMessages.sourceTooLarge(MAX_SOURCE_CHARS);
        }
        if (!Language.isSupported(language)) {
            return FailureMessages.unsupportedLanguage(language);
        }
        return null;
    }
}
