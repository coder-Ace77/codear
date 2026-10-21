package com.codear.engine.judge;

import static com.codear.engine.constants.JudgeDefaults.MAX_SOURCE_CHARS;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.judge.CustomRunResult;
import com.codear.engine.dto.judge.ExpectedTest;
import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.dto.judge.JudgeReport;
import com.codear.engine.dto.judge.JudgeRequest;
import com.codear.engine.dto.judge.TestLocation;
import com.codear.engine.dto.sandbox.SandboxRequest;
import com.codear.engine.dto.sandbox.SandboxResult.TestRun;
import com.codear.engine.dto.sandbox.SandboxResult;
import com.codear.engine.enums.CompileStatus;
import com.codear.engine.enums.Language;
import com.codear.engine.enums.RunOutcome;
import com.codear.engine.exception.SandboxException;
import com.codear.engine.exception.UnsupportedLanguageException;
import com.codear.engine.sandbox.SandboxRunner;
import com.codear.engine.util.judge.FailureMessages;

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
    public JudgeReport judge(JudgeRequest request) {
        List<ExpectedTest> tests = request.getTests();
        int total = tests.size();

        if (tests.isEmpty()) {
            return JudgeReport.systemError(0, FailureMessages.noTests());
        }

        Optional<JudgeReport> rejected = reject(request.getCode(), total);
        if (rejected.isPresent()) {
            return rejected.get();
        }

        Language language;
        try {
            language = Language.from(request.getLanguage());
        } catch (UnsupportedLanguageException e) {
            return JudgeReport.compileError(total, FailureMessages.unsupportedLanguage(request.getLanguage()), "");
        }

        JudgeLimits limits = JudgeLimits.forProblem(request.getConstraints(), tests);
        List<String> inputs = tests.stream().map(ExpectedTest::getInput).toList();

        SandboxResult result;
        try {
            result = sandbox.run(new SandboxRequest(language, request.getCode(), inputs, limits));
        } catch (SandboxException e) {
            log.error("Sandbox failed: {}", e.getMessage(), e);
            return JudgeReport.systemError(total, FailureMessages.judgeFailure());
        }

        return new VerdictResolver(request.getComparator()).resolve(result, tests, limits);
    }

    /** Runs the code on one input a person typed, with no expected answer. */
    public CustomRunResult runCustom(String code, String languageName, String input, ResourceConstraints constraints) {
        Optional<JudgeReport> rejected = reject(code, 1);
        if (rejected.isPresent()) {
            return new CustomRunResult(rejected.get().getMessage(), false);
        }

        Language language;
        try {
            language = Language.from(languageName);
        } catch (UnsupportedLanguageException e) {
            return new CustomRunResult(FailureMessages.unsupportedLanguage(languageName), false);
        }

        JudgeLimits limits = JudgeLimits.forCustomRun(constraints);
        SandboxResult result;
        try {
            result = sandbox.run(new SandboxRequest(language, code, List.of(input == null ? "" : input), limits));
        } catch (SandboxException e) {
            log.error("Sandbox failed on a custom run: {}", e.getMessage(), e);
            return new CustomRunResult(FailureMessages.judgeFailure(), false);
        }
        return describeCustomRun(result, limits);
    }

    private static Optional<JudgeReport> reject(String code, int totalTests) {
        if (code == null || code.isBlank()) {
            return Optional.of(JudgeReport.compileError(totalTests, FailureMessages.emptySource(), ""));
        }
        if (code.length() > MAX_SOURCE_CHARS) {
            return Optional.of(JudgeReport.compileError(totalTests, FailureMessages.sourceTooLarge(MAX_SOURCE_CHARS), ""));
        }
        return Optional.empty();
    }

    private CustomRunResult describeCustomRun(SandboxResult result, JudgeLimits limits) {
        if (result.getCompile().getStatus() != CompileStatus.OK) {
            String message = result.getCompile().getStatus() == CompileStatus.TIMED_OUT
                    ? FailureMessages.compileTimeout(limits.getCompileTimeoutSeconds())
                    : FailureMessages.compileError(result.getCompile().getOutput());
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
            case OK -> new CustomRunResult(run.getStdout(), true);
            case TIME_LIMIT -> new CustomRunResult(FailureMessages.timeLimit(where, limits.getTimeLimitMs()), false);
            case MEMORY_LIMIT -> new CustomRunResult(FailureMessages.memoryLimit(where, limits.getMemoryLimitMb()), false);
            case OUTPUT_LIMIT -> new CustomRunResult(FailureMessages.outputLimit(where), false);
            case RUNTIME_ERROR -> new CustomRunResult(
                    FailureMessages.runtimeError(where, true, run.getExitCode(), run.getStderrTail()), false);
        };
    }
}
