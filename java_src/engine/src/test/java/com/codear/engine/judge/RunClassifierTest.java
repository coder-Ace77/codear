package com.codear.engine.judge;

import static com.codear.engine.judge.Fixtures.LIMITS;
import static com.codear.engine.judge.Fixtures.run;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.codear.engine.sandbox.RunOutcome;
import com.codear.engine.sandbox.TestRun;

/** The rules that turn "how the process ended" into "which limit it hit". Limits: 1000 ms CPU, 256 MB, 64 KB output, 3 s wall. */
class RunClassifierTest {

    private final RunClassifier classifier = new RunClassifier();

    private RunOutcome classify(TestRun run) {
        return classifier.classify(run, LIMITS);
    }

    @Test
    void aCleanRunWithinEveryLimitIsOk() {
        assertEquals(RunOutcome.OK, classify(run(0, 0, 100, 80, 50_000, "3", "")));
    }

    @Test
    void aCleanRunThatUsedTooMuchCpuTimeIsTooSlow() {
        assertEquals(RunOutcome.TIME_LIMIT, classify(run(0, 0, 1500, 1200, 8_000, "3", "")));
    }

    @Test
    void aCleanRunExactlyAtTheTimeLimitIsStillOk() {
        assertEquals(RunOutcome.OK, classify(run(0, 0, 1000, 1000, 8_000, "3", "")));
    }

    @Test
    void aCleanRunThatUsedTooMuchMemoryIsOverTheLimit() {
        assertEquals(RunOutcome.MEMORY_LIMIT, classify(run(0, 0, 100, 80, 300 * 1024, "3", "")));
    }

    @Test
    void aCleanRunThatFilledTheOutputCapIsAFlood() {
        assertEquals(RunOutcome.OUTPUT_LIMIT, classify(run(0, 0, 100, 80, 8_000, "x".repeat(64 * 1024), "")));
    }

    @ParameterizedTest(name = "exit {0} is a time limit")
    @CsvSource({"124", "152"})
    void theWallAndCpuLimitExitCodesAreTimeLimits(int exit) {
        assertEquals(RunOutcome.TIME_LIMIT, classify(run(0, exit, 3000, 10, 8_000, "", "")));
    }

    @Test
    void theOutputLimitSignalIsAnOutputLimit() {
        assertEquals(RunOutcome.OUTPUT_LIMIT, classify(run(0, 153, 100, 80, 8_000, "partial", "")));
    }

    @Test
    void killedAfterUsingItsCpuTimeIsATimeLimit() {
        assertEquals(RunOutcome.TIME_LIMIT, classify(run(0, 137, 1000, 990, 8_000, "", "")));
    }

    @Test
    void killedAfterUsingItsWallTimeIsATimeLimit() {
        assertEquals(RunOutcome.TIME_LIMIT, classify(run(0, 137, 3000, 5, 8_000, "", "")));
    }

    @Test
    void killedEarlyWithLittleCpuTimeIsTheMemoryKiller() {
        assertEquals(RunOutcome.MEMORY_LIMIT, classify(run(0, 137, 400, 200, 250 * 1024, "", "")));
    }

    @ParameterizedTest(name = "stderr \"{0}\" means out of memory")
    @CsvSource({
            "terminate called after throwing an instance of 'std::bad_alloc'",
            "MemoryError",
            "fatal: Cannot allocate memory",
            "Out of memory"})
    void anErrorMessageAboutMemoryIsAMemoryLimit(String stderr) {
        assertEquals(RunOutcome.MEMORY_LIMIT, classify(run(0, 134, 100, 80, 200_000, "", stderr)));
    }

    @Test
    void aProgramOverTheMemoryLimitThatCrashedIsAMemoryLimit() {
        assertEquals(RunOutcome.MEMORY_LIMIT, classify(run(0, 1, 100, 80, 300 * 1024, "", "")));
    }

    @ParameterizedTest(name = "exit {0} is a runtime error")
    @CsvSource({"1", "2", "3", "134", "136", "139"})
    void anyOtherNonZeroExitIsARuntimeError(int exit) {
        assertEquals(RunOutcome.RUNTIME_ERROR, classify(run(0, exit, 100, 80, 8_000, "", "boom")));
    }

    @Test
    void anOutputFloodWinsOverTheExitCodePythonReportsForIt() {
        // Python turns "file too large" into an ordinary exception and exits 120
        assertEquals(RunOutcome.OUTPUT_LIMIT, classify(run(0, 120, 100, 80, 8_000, "x".repeat(64 * 1024), "OSError")));
    }
}
