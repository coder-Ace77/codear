package com.codear.engine.judge;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.sandbox.Language;
import com.codear.engine.sandbox.RunOutcome;
import com.codear.engine.sandbox.SandboxException;
import com.codear.engine.sandbox.SandboxRequest;
import com.codear.engine.sandbox.SandboxResult;
import com.codear.engine.sandbox.SandboxRunner;
import com.codear.engine.sandbox.TestRun;
import com.codear.engine.service.TestProgressListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Judges a submission end to end: check the code, run it in the sandbox, decide the verdict. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final SandboxRunner sandbox;
    private final RunClassifier classifier = new RunClassifier();

    /**
     * Never throws for a bad submission or a sandbox failure: both come back as a report, so the person always
     * gets an answer and the queue message is not lost.
     */
    public JudgeReport judge(JudgeRequest request, TestProgressListener progress) {
        List<ExpectedTest> tests = request.tests();
        int total = tests.size();

        if (tests.isEmpty()) {
            return JudgeReport.systemError(0, FailureMessages.noTests());
        }

        Optional<JudgeReport> rejected = CodeValidator.reject(request.code(), total);
        if (rejected.isPresent()) {
            return rejected.get();
        }

        Language language;
        try {
            language = Language.from(request.language());
        } catch (Language.UnsupportedLanguageException e) {
            return JudgeReport.compileError(total, FailureMessages.unsupportedLanguage(request.language()), "");
        }

        JudgeLimits limits = JudgeLimitsFactory.forProblem(request.constraints(), tests);
        List<String> inputs = tests.stream().map(ExpectedTest::input).toList();

        SandboxResult result;
        try {
            result = sandbox.run(new SandboxRequest(language, request.code(), inputs, limits), progress);
        } catch (SandboxException e) {
            log.error("Sandbox failed: {}", e.getMessage(), e);
            return JudgeReport.systemError(total, FailureMessages.judgeFailure());
        }

        if (progress != null) {
            progress.judging();
        }
        return new VerdictResolver(request.comparator()).resolve(result, tests, limits);
    }

    /** Runs the code on one input a person typed, with no expected answer. */
    public CustomRunResult runCustom(String code, String languageName, String input, ResourceConstraints constraints) {
        Optional<JudgeReport> rejected = CodeValidator.reject(code, 1);
        if (rejected.isPresent()) {
            return new CustomRunResult(rejected.get().message(), false);
        }

        Language language;
        try {
            language = Language.from(languageName);
        } catch (Language.UnsupportedLanguageException e) {
            return new CustomRunResult(FailureMessages.unsupportedLanguage(languageName), false);
        }

        JudgeLimits limits = JudgeLimitsFactory.forCustomRun(constraints);
        SandboxResult result;
        try {
            result = sandbox.run(new SandboxRequest(language, code, List.of(input == null ? "" : input), limits), null);
        } catch (SandboxException e) {
            log.error("Sandbox failed on a custom run: {}", e.getMessage(), e);
            return new CustomRunResult(FailureMessages.judgeFailure(), false);
        }
        return describeCustomRun(result, limits);
    }

    private CustomRunResult describeCustomRun(SandboxResult result, JudgeLimits limits) {
        if (!result.compile().ok()) {
            String message = result.compile().timedOut()
                    ? FailureMessages.compileTimeout(limits.compileTimeoutSeconds())
                    : FailureMessages.compileError(result.compile().output());
            return new CustomRunResult(message, false);
        }
        Optional<TestRun> recorded = result.runFor(0);
        if (recorded.isEmpty()) {
            return new CustomRunResult(FailureMessages.didNotFinish(TestLocation.customRun()), false);
        }
        TestRun run = recorded.get();
        TestLocation where = TestLocation.customRun();
        RunOutcome outcome = classifier.classify(run, limits);
        return switch (outcome) {
            case OK -> new CustomRunResult(run.stdout(), true);
            case TIME_LIMIT -> new CustomRunResult(FailureMessages.timeLimit(where, limits.timeLimitMs()), false);
            case MEMORY_LIMIT -> new CustomRunResult(FailureMessages.memoryLimit(where, limits.memoryLimitMb()), false);
            case OUTPUT_LIMIT -> new CustomRunResult(FailureMessages.outputLimit(where), false);
            case RUNTIME_ERROR -> new CustomRunResult(
                    FailureMessages.runtimeError(where, true, run.exitCode(), run.stderrTail()), false);
        };
    }
}
