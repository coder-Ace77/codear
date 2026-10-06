package com.codear.engine.judge;

import static com.codear.engine.constants.JudgeDefaults.COMPILE_TIMEOUT_SECONDS;
import static com.codear.engine.constants.JudgeDefaults.DEFAULT_MEMORY_LIMIT_MB;
import static com.codear.engine.constants.JudgeDefaults.DEFAULT_TIME_LIMIT_MS;
import static com.codear.engine.constants.JudgeDefaults.MAX_OUTPUT_LIMIT_BYTES;
import static com.codear.engine.constants.JudgeDefaults.MIN_OUTPUT_LIMIT_BYTES;
import static com.codear.engine.constants.JudgeDefaults.OUTPUT_LIMIT_FACTOR;
import static com.codear.engine.constants.JudgeDefaults.OUTPUT_LIMIT_SLACK_BYTES;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.codear.engine.dto.ResourceConstraints;

/** Works out the limits for a submission from its problem and tests. */
public final class JudgeLimitsFactory {

    private JudgeLimitsFactory() {
    }

    public static JudgeLimits forProblem(ResourceConstraints constraints, List<ExpectedTest> tests) {
        long timeMs = constraints != null && positive(constraints.getTimeLimitMs())
                ? constraints.getTimeLimitMs()
                : DEFAULT_TIME_LIMIT_MS;
        int memoryMb = constraints != null && constraints.getMemoryLimitMb() != null && constraints.getMemoryLimitMb() > 0
                ? constraints.getMemoryLimitMb()
                : DEFAULT_MEMORY_LIMIT_MB;
        return new JudgeLimits(timeMs, memoryMb, outputLimitFor(tests), COMPILE_TIMEOUT_SECONDS);
    }

    /** Limits for running a person's own input, where there is no expected output to size the cap from. */
    public static JudgeLimits forCustomRun(ResourceConstraints constraints) {
        return forProblem(constraints, List.of());
    }

    /**
     * A program may write several times the largest expected output before it counts as a flood: a program that
     * prints far more than any correct answer is wrong anyway, and the cap keeps one bad run from filling memory.
     */
    static long outputLimitFor(List<ExpectedTest> tests) {
        long largest = tests.stream()
                .mapToLong(t -> t.expectedOutput().getBytes(StandardCharsets.UTF_8).length)
                .max()
                .orElse(0);
        long wanted = largest * OUTPUT_LIMIT_FACTOR + OUTPUT_LIMIT_SLACK_BYTES;
        return Math.max(MIN_OUTPUT_LIMIT_BYTES, Math.min(MAX_OUTPUT_LIMIT_BYTES, wanted));
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }
}
