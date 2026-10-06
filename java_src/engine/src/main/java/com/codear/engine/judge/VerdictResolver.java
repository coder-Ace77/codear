package com.codear.engine.judge;

import static com.codear.engine.constants.JudgeDefaults.MAX_STDERR_BYTES_KEPT;

import java.util.List;
import java.util.Optional;

import com.codear.engine.checker.OutputComparator;
import com.codear.engine.sandbox.RunOutcome;
import com.codear.engine.sandbox.SandboxResult;
import com.codear.engine.sandbox.TestRun;

/**
 * Turns what the sandbox recorded into a verdict. Tests are judged in order and the first one that fails decides:
 * a run that hit a limit or crashed is judged on how it ran, one that finished cleanly on whether its answer matches.
 * Pure logic: it never touches Docker, the database or the clock.
 */
public final class VerdictResolver {

    private final RunClassifier classifier = new RunClassifier();
    private final OutputComparator comparator;

    public VerdictResolver(OutputComparator comparator) {
        this.comparator = comparator;
    }

    public JudgeReport resolve(SandboxResult result, List<ExpectedTest> tests, JudgeLimits limits) {
        int total = tests.size();
        if (!result.compile().ok()) {
            return compileFailure(result, total, limits);
        }

        long maxTimeMs = 0;
        long peakKb = 0;
        for (ExpectedTest test : tests) {
            Optional<TestRun> recorded = result.runFor(test.number() - 1);
            if (recorded.isEmpty()) {
                return unfinished(result, test, total, limits, maxTimeMs, peakKb);
            }
            TestRun run = recorded.get();
            maxTimeMs = Math.max(maxTimeMs, run.cpuMs());
            peakKb = Math.max(peakKb, run.peakKb());

            TestLocation where = new TestLocation(test.number(), total);
            RunOutcome outcome = classifier.classify(run, limits);
            if (outcome != RunOutcome.OK) {
                return failedRun(outcome, where, test, run, limits, maxTimeMs, peakKb);
            }
            if (!comparator.matches(test.expectedOutput(), run.stdout())) {
                String message = FailureMessages.wrongAnswer(where, test.visible(), test.expectedOutput(), run.stdout());
                return JudgeReport.failedOnTest(Verdict.WRONG_ANSWER, test.number(), total, message, "", maxTimeMs, peakKb);
            }
        }
        return JudgeReport.accepted(total, maxTimeMs, peakKb);
    }

    private JudgeReport compileFailure(SandboxResult result, int total, JudgeLimits limits) {
        String output = result.compile().output();
        String message = result.compile().timedOut()
                ? FailureMessages.compileTimeout(limits.compileTimeoutSeconds())
                : FailureMessages.compileError(output);
        return JudgeReport.compileError(total, message, keepTail(output));
    }

    private JudgeReport failedRun(RunOutcome outcome, TestLocation where, ExpectedTest test, TestRun run,
            JudgeLimits limits, long maxTimeMs, long peakKb) {
        Verdict verdict;
        String message;
        switch (outcome) {
            case TIME_LIMIT -> {
                verdict = Verdict.TIME_LIMIT_EXCEEDED;
                message = FailureMessages.timeLimit(where, limits.timeLimitMs());
            }
            case MEMORY_LIMIT -> {
                verdict = Verdict.MEMORY_LIMIT_EXCEEDED;
                message = FailureMessages.memoryLimit(where, limits.memoryLimitMb());
            }
            case OUTPUT_LIMIT -> {
                verdict = Verdict.OUTPUT_LIMIT_EXCEEDED;
                message = FailureMessages.outputLimit(where);
            }
            default -> {
                verdict = Verdict.RUNTIME_ERROR;
                message = FailureMessages.runtimeError(where, test.visible(), run.exitCode(), run.stderrTail());
            }
        }
        return JudgeReport.failedOnTest(verdict, test.number(), where.total(), message, keepTail(run.stderrTail()),
                maxTimeMs, peakKb);
    }

    /**
     * The container ended before this test was recorded: it was cut off by the overall timeout or killed for memory.
     */
    private JudgeReport unfinished(SandboxResult result, ExpectedTest test, int total, JudgeLimits limits,
            long maxTimeMs, long peakKb) {
        TestLocation where = new TestLocation(test.number(), total);
        if (result.oomKilled()) {
            return JudgeReport.failedOnTest(Verdict.MEMORY_LIMIT_EXCEEDED, test.number(), total,
                    FailureMessages.memoryLimit(where, limits.memoryLimitMb()), "", maxTimeMs, peakKb);
        }
        return JudgeReport.failedOnTest(Verdict.TIME_LIMIT_EXCEEDED, test.number(), total,
                FailureMessages.timeLimit(where, limits.timeLimitMs()), "", maxTimeMs, peakKb);
    }

    private static String keepTail(String text) {
        if (text == null || text.length() <= MAX_STDERR_BYTES_KEPT) {
            return text == null ? "" : text;
        }
        return text.substring(text.length() - MAX_STDERR_BYTES_KEPT);
    }
}
