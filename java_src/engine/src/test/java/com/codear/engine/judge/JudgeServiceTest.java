package com.codear.engine.judge;

import static com.codear.engine.judge.Fixtures.finished;
import static com.codear.engine.judge.Fixtures.hidden;
import static com.codear.engine.judge.Fixtures.ok;
import static com.codear.engine.judge.Fixtures.run;
import static com.codear.engine.judge.Fixtures.sample;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codear.engine.checker.TokenComparator;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.judge.RunReport;
import com.codear.engine.dto.judge.ExpectedTest;
import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.dto.judge.JudgeReport;
import com.codear.engine.enums.Verdict;

class JudgeServiceTest {

    private final JudgeService service = new JudgeService();
    private final JudgeLimits limits = JudgeLimits.forCustomRun(new ResourceConstraints(2000L, 128, "Medium"));

    @Test
    void matchingOutputOnEveryTestIsAccepted() {
        List<ExpectedTest> tests = List.of(sample(1, "1"), hidden(2, "2"));

        JudgeReport report = service.judge(finished(ok(0, "1"), ok(1, "2")), tests, limits, new TokenComparator());

        assertEquals(Verdict.ACCEPTED, report.getVerdict());
    }

    @Test
    void aWrongOutputIsAWrongAnswer() {
        List<ExpectedTest> tests = List.of(sample(1, "1"), hidden(2, "2"));

        JudgeReport report = service.judge(finished(ok(0, "1"), ok(1, "3")), tests, limits, new TokenComparator());

        assertEquals(Verdict.WRONG_ANSWER, report.getVerdict());
        assertEquals(2, report.getFailedTest());
    }

    @Test
    void aCustomRunReturnsTheOutputOfAProgramThatRanCleanly() {
        RunReport result = service.judgeCustomRun(finished(ok(0, "hello\n")), limits);

        assertTrue(result.isSuccess());
        assertEquals("hello\n", result.getText());
    }

    @Test
    void aCustomRunExplainsEachWayItCanFail() {
        assertEquals("Compilation failed:\ncode.cpp:1:1: error: stray",
                service.judgeCustomRun(Fixtures.compileFailed(1, "/tmp/build/code.cpp:1:1: error: stray"), limits)
                        .getText());

        assertTrue(service.judgeCustomRun(finished(run(0, 139, 80, 20, 4_000, "", "")), limits).getText()
                .startsWith("Runtime error: segmentation fault"));

        assertEquals("Time limit exceeded (limit 2000 ms).",
                service.judgeCustomRun(finished(run(0, 124, 3000, 10, 4_000, "", "")), limits).getText());

        assertEquals("The program did not finish.", service.judgeCustomRun(finished(), limits).getText());
    }

    @Test
    void aCustomRunThatPrintsMoreThanTheOutputCapIsAFlood() {
        RunReport result = service.judgeCustomRun(finished(ok(0, "y".repeat(200_000))), limits);

        assertFalse(result.isSuccess());
        assertTrue(result.getText().startsWith("Output limit exceeded"));
    }
}
