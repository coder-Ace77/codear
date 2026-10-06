package com.codear.engine.judge;

import static com.codear.engine.judge.Fixtures.LIMITS;
import static com.codear.engine.judge.Fixtures.cutOff;
import static com.codear.engine.judge.Fixtures.exitedWith;
import static com.codear.engine.judge.Fixtures.finished;
import static com.codear.engine.judge.Fixtures.hidden;
import static com.codear.engine.judge.Fixtures.ok;
import static com.codear.engine.judge.Fixtures.run;
import static com.codear.engine.judge.Fixtures.sample;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codear.engine.checker.ComparatorFactory;
import com.codear.engine.enums.RunStatus;

class VerdictResolverTest {

    private final VerdictResolver resolver = new VerdictResolver(ComparatorFactory.defaultComparator());

    private final List<ExpectedTest> threeTests = List.of(sample(1, "1"), hidden(2, "2"), hidden(3, "3"));

    @Test
    void everyTestMatchingIsAccepted() {
        JudgeReport report = resolver.resolve(finished(ok(0, "1"), ok(1, "2"), ok(2, "3")), threeTests, LIMITS);

        assertEquals(Verdict.ACCEPTED, report.verdict());
        assertEquals(RunStatus.PASSED, report.verdict().runStatus());
        assertEquals(3, report.passedTests());
        assertNull(report.failedTest());
        assertEquals(JudgeReport.ALL_PASSED, report.message());
    }

    @Test
    void theReportCarriesTheWorstTimeAndMemoryOfAllTests() {
        JudgeReport report = resolver.resolve(finished(
                run(0, 0, 50, 10, 4_000, "1", ""),
                run(1, 0, 90, 70, 9_000, "2", ""),
                run(2, 0, 60, 30, 5_000, "3", "")), threeTests, LIMITS);

        assertEquals(70, report.maxTimeMs());
        assertEquals(9_000, report.peakMemoryKb());
        assertEquals("8.79MB", report.memoryLabel());
    }

    @Test
    void theFirstWrongAnswerDecidesAndLaterTestsAreNotConsidered() {
        JudgeReport report = resolver.resolve(finished(ok(0, "1"), ok(1, "WRONG"), ok(2, "ALSO WRONG")), threeTests, LIMITS);

        assertEquals(Verdict.WRONG_ANSWER, report.verdict());
        assertEquals(2, report.failedTest());
        assertEquals(1, report.passedTests());
        assertEquals(3, report.totalTests());
    }

    @Test
    void aWrongAnswerOnAHiddenTestDoesNotLeakTheExpectedOutput() {
        JudgeReport report = resolver.resolve(finished(ok(0, "1"), ok(1, "WRONG")),
                List.of(sample(1, "1"), hidden(2, "THE-SECRET-ANSWER")), LIMITS);

        assertFalse(report.message().contains("THE-SECRET-ANSWER"));
        assertTrue(report.message().contains("hidden test"));
    }

    @Test
    void aWrongAnswerOnASampleShowsExpectedAndGot() {
        JudgeReport report = resolver.resolve(finished(ok(0, "NO")), List.of(sample(1, "YES")), LIMITS);

        assertTrue(report.message().contains("Expected: YES"));
        assertTrue(report.message().contains("Got: NO"));
    }

    @Test
    void spacingAndLineBreaksDoNotMakeAnAnswerWrong() {
        JudgeReport report = resolver.resolve(finished(ok(0, "  1  2\n\n3 \n")), List.of(sample(1, "1\n2\n3")), LIMITS);

        assertEquals(Verdict.ACCEPTED, report.verdict());
    }

    @Test
    void aCompileErrorEndsEverythingBeforeAnyTest() {
        JudgeReport report = resolver.resolve(
                Fixtures.compileFailed(1, "/tmp/build/code.cpp:1:1: error: stray"), threeTests, LIMITS);

        assertEquals(Verdict.COMPILE_ERROR, report.verdict());
        assertNull(report.failedTest());
        assertEquals(0, report.passedTests());
        assertTrue(report.message().contains("code.cpp:1:1: error: stray"));
        assertFalse(report.message().contains("/tmp/build/"));
    }

    @Test
    void aCompilerThatTimedOutIsExplained() {
        JudgeReport report = resolver.resolve(Fixtures.compileFailed(124, ""), threeTests, LIMITS);

        assertEquals(Verdict.COMPILE_ERROR, report.verdict());
        assertEquals("Compilation took longer than 20 seconds.", report.message());
    }

    @Test
    void aCrashIsARuntimeErrorOnThatTest() {
        JudgeReport report = resolver.resolve(
                finished(ok(0, "1"), run(1, 139, 80, 20, 4_000, "", "")), threeTests, LIMITS);

        assertEquals(Verdict.RUNTIME_ERROR, report.verdict());
        assertEquals(2, report.failedTest());
        assertEquals(1, report.passedTests());
        assertTrue(report.message().contains("segmentation fault"));
    }

    @Test
    void aTimeoutIsATimeLimitVerdictWithTheLimit() {
        JudgeReport report = resolver.resolve(finished(run(0, 124, 3000, 10, 4_000, "", "")), threeTests, LIMITS);

        assertEquals(Verdict.TIME_LIMIT_EXCEEDED, report.verdict());
        assertEquals(1, report.failedTest());
        assertEquals("Time limit exceeded on test 1 of 3 (limit 1000 ms).", report.message());
    }

    @Test
    void aSlowButCorrectRunIsStillTooSlow() {
        JudgeReport report = resolver.resolve(finished(run(0, 0, 1700, 1600, 4_000, "1", "")), threeTests, LIMITS);

        assertEquals(Verdict.TIME_LIMIT_EXCEEDED, report.verdict());
    }

    @Test
    void memoryAndOutputLimitsGetTheirOwnVerdicts() {
        assertEquals(Verdict.MEMORY_LIMIT_EXCEEDED, resolver.resolve(
                finished(run(0, 134, 80, 20, 4_000, "", "std::bad_alloc")), threeTests, LIMITS).verdict());
        assertEquals(Verdict.OUTPUT_LIMIT_EXCEEDED, resolver.resolve(
                finished(run(0, 153, 80, 20, 4_000, "x".repeat(64 * 1024), "")), threeTests, LIMITS).verdict());
    }

    @Test
    void theErrorOutputIsKeptAsDiagnosticsButOnlyItsTail() {
        JudgeReport report = resolver.resolve(finished(exitedWith(1, "e".repeat(20_000))), threeTests, LIMITS);

        assertEquals(4096, report.diagnostics().length());
    }

    @Test
    void aMissingTestAfterTheContainerWasCutOffIsATimeLimit() {
        JudgeReport report = resolver.resolve(cutOff(false, ok(0, "1")), threeTests, LIMITS);

        assertEquals(Verdict.TIME_LIMIT_EXCEEDED, report.verdict());
        assertEquals(2, report.failedTest());
    }

    @Test
    void aMissingTestAfterAnOomKillIsAMemoryLimit() {
        JudgeReport report = resolver.resolve(cutOff(true), threeTests, LIMITS);

        assertEquals(Verdict.MEMORY_LIMIT_EXCEEDED, report.verdict());
        assertEquals(1, report.failedTest());
    }

    @Test
    void nothingPrintedFailsAgainstAnExpectedAnswer() {
        JudgeReport report = resolver.resolve(finished(ok(0, "")), List.of(sample(1, "20")), LIMITS);

        assertEquals(Verdict.WRONG_ANSWER, report.verdict());
    }
}
