package com.codear.engine.judge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codear.engine.dto.ResourceConstraints;

class JudgeLimitsTest {

    @Test
    void cpuIsRoundedUpToWholeSecondsAndWallTimeIsAGenerousMultiple() {
        assertEquals(1, new JudgeLimits(1000, 256, 1, 20).cpuLimitSeconds());
        assertEquals(2, new JudgeLimits(1500, 256, 1, 20).cpuLimitSeconds());
        assertEquals(3, new JudgeLimits(1000, 256, 1, 20).wallLimitSeconds());
        assertEquals(5, new JudgeLimits(2000, 256, 1, 20).wallLimitSeconds());
    }

    @Test
    void theContainerGetsHeadroomForTheCompiler() {
        JudgeLimits limits = new JudgeLimits(1000, 256, 1, 20);

        assertEquals(256L * 1024 * 1024, limits.memoryLimitBytes());
        assertEquals(256L * 1024, limits.memoryLimitKb());
        assertEquals(512, limits.containerMemoryMb());
    }

    @Test
    void theOverallTimeoutCoversEveryTestHittingItsWallLimit() {
        // 20 s compile + 26 tests * (3 s wall + 2 s kill grace) + 30 s startup
        assertEquals(20 + 26 * 5 + 30, new JudgeLimits(1000, 256, 1, 20).totalTimeoutSeconds(26));
    }

    @Test
    void aProblemWithoutLimitsGetsTheDefaults() {
        JudgeLimits limits = JudgeLimitsFactory.forProblem(null, List.of());

        assertEquals(1000, limits.timeLimitMs());
        assertEquals(256, limits.memoryLimitMb());
        assertEquals(20, limits.compileTimeoutSeconds());
    }

    @Test
    void aProblemsOwnLimitsAreUsedAndNonsenseFallsBackToTheDefaults() {
        JudgeLimits own = JudgeLimitsFactory.forProblem(new ResourceConstraints(2000L, 128, "Hard"), List.of());
        JudgeLimits broken = JudgeLimitsFactory.forProblem(new ResourceConstraints(0L, -5, "Hard"), List.of());

        assertEquals(2000, own.timeLimitMs());
        assertEquals(128, own.memoryLimitMb());
        assertEquals(1000, broken.timeLimitMs());
        assertEquals(256, broken.memoryLimitMb());
    }

    @Test
    void theOutputCapIsAMultipleOfTheLargestExpectedAnswerWithinSaneBounds() {
        assertEquals(64 * 1024, JudgeLimitsFactory.outputLimitFor(List.of()));
        assertEquals(64 * 1024, JudgeLimitsFactory.outputLimitFor(List.of(new ExpectedTest(1, "", "YES", true))));

        long fortyThousandLines = JudgeLimitsFactory.outputLimitFor(
                List.of(new ExpectedTest(1, "", "YES\n".repeat(40_000), false)));
        assertEquals(160_000L * 4 + 4096, fortyThousandLines);

        assertEquals(8L * 1024 * 1024, JudgeLimitsFactory.outputLimitFor(
                List.of(new ExpectedTest(1, "", "x".repeat(5_000_000), false))));
    }
}
