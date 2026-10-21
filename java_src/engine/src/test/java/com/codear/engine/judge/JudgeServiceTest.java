package com.codear.engine.judge;

import static com.codear.engine.judge.Fixtures.finished;
import static com.codear.engine.judge.Fixtures.hidden;
import static com.codear.engine.judge.Fixtures.ok;
import static com.codear.engine.judge.Fixtures.run;
import static com.codear.engine.judge.Fixtures.sample;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.codear.engine.checker.ComparatorFactory;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.judge.CustomRunResult;
import com.codear.engine.dto.judge.ExpectedTest;
import com.codear.engine.dto.judge.JudgeReport;
import com.codear.engine.dto.judge.JudgeRequest;
import com.codear.engine.dto.sandbox.SandboxRequest;
import com.codear.engine.dto.sandbox.SandboxResult;
import com.codear.engine.enums.Verdict;
import com.codear.engine.exception.SandboxException;
import com.codear.engine.sandbox.SandboxRunner;

class JudgeServiceTest {

    /** A sandbox that returns a canned result and remembers what it was asked to run. */
    private static final class FakeSandbox implements SandboxRunner {
        final List<SandboxRequest> requests = new ArrayList<>();
        SandboxResult result;
        RuntimeException failure;

        @Override
        public SandboxResult run(SandboxRequest request) {
            requests.add(request);
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private final FakeSandbox sandbox = new FakeSandbox();
    private final JudgeService service = new JudgeService(sandbox);
    private final ResourceConstraints constraints = new ResourceConstraints(2000L, 128, "Medium");
    private final List<ExpectedTest> tests = List.of(sample(1, "1"), hidden(2, "2"));

    private JudgeRequest request(String code, String language, List<ExpectedTest> tests) {
        return new JudgeRequest(code, language, tests, constraints, ComparatorFactory.defaultComparator());
    }

    @Test
    void aGoodSubmissionIsRunWithTheProblemsLimitsAndAccepted() {
        sandbox.result = finished(ok(0, "1"), ok(1, "2"));

        JudgeReport report = service.judge(request("print(1)", "python", tests));

        assertEquals(Verdict.ACCEPTED, report.getVerdict());
        SandboxRequest sent = sandbox.requests.get(0);
        assertEquals(List.of("in1", "in2"), sent.getInputs());
        assertEquals(2000, sent.getLimits().getTimeLimitMs());
        assertEquals(128, sent.getLimits().getMemoryLimitMb());
    }

    @Test
    void emptySourceNeverStartsASandbox() {
        for (String code : new String[] {"", "   \n\t", null}) {
            JudgeReport report = service.judge(request(code, "python", tests));

            assertEquals(Verdict.COMPILE_ERROR, report.getVerdict());
            assertEquals("The source code is empty.", report.getMessage());
        }
        assertTrue(sandbox.requests.isEmpty());
    }

    @Test
    void oversizedSourceIsRejected() {
        JudgeReport report = service.judge(request("x".repeat(70_000), "python", tests));

        assertEquals(Verdict.COMPILE_ERROR, report.getVerdict());
        assertTrue(report.getMessage().contains("too long"));
        assertTrue(sandbox.requests.isEmpty());
    }

    @Test
    void anUnsupportedLanguageIsExplained() {
        JudgeReport report = service.judge(request("code", "brainfuck", tests));

        assertEquals(Verdict.COMPILE_ERROR, report.getVerdict());
        assertEquals("Unsupported language: brainfuck.", report.getMessage());
        assertTrue(sandbox.requests.isEmpty());
    }

    @Test
    void aProblemWithoutTestsIsAJudgeErrorNeverAnAccept() {
        JudgeReport report = service.judge(request("print(1)", "python", List.of()));

        assertEquals(Verdict.SYSTEM_ERROR, report.getVerdict());
        assertFalse(report.getVerdict().isAccepted());
        assertTrue(sandbox.requests.isEmpty());
    }

    @Test
    void aSandboxFailureBecomesASystemErrorReportNotAnException() {
        sandbox.failure = new SandboxException("docker is down");

        JudgeReport report = service.judge(request("print(1)", "python", tests));

        assertEquals(Verdict.SYSTEM_ERROR, report.getVerdict());
        assertFalse(report.getMessage().contains("docker"), "internal details must not reach the person");
    }

    @Test
    void aCustomRunReturnsTheOutputOfAProgramThatRanCleanly() {
        sandbox.result = finished(ok(0, "hello\n"));

        CustomRunResult result = service.runCustom("print('hello')", "python", "", constraints);

        assertTrue(result.isSuccess());
        assertEquals("hello\n", result.getText());
    }

    @Test
    void aCustomRunExplainsEachWayItCanFail() {
        sandbox.result = Fixtures.compileFailed(1, "/tmp/build/code.cpp:1:1: error: stray");
        assertEquals("Compilation failed:\ncode.cpp:1:1: error: stray",
                service.runCustom("x", "cpp", "", constraints).getText());

        sandbox.result = finished(run(0, 139, 80, 20, 4_000, "", ""));
        assertTrue(service.runCustom("x", "cpp", "", constraints).getText().startsWith("Runtime error: segmentation fault"));

        sandbox.result = finished(run(0, 124, 3000, 10, 4_000, "", ""));
        assertEquals("Time limit exceeded (limit 2000 ms).", service.runCustom("x", "cpp", "", constraints).getText());

        sandbox.result = finished();
        assertEquals("The program did not finish.", service.runCustom("x", "cpp", "", constraints).getText());
    }

    @Test
    void aCustomRunWithEmptyCodeOrABrokenSandboxIsExplained() {
        assertEquals("The source code is empty.", service.runCustom(" ", "python", "", constraints).getText());

        sandbox.failure = new SandboxException("boom");
        CustomRunResult result = service.runCustom("print(1)", "python", "", constraints);
        assertFalse(result.isSuccess());
        assertFalse(result.getText().contains("boom"));
    }

    @Test
    void aCustomRunThatPrintsMoreThanTheOutputCapIsAFloodAndNullInputIsAnEmptyInput() {
        sandbox.result = finished(ok(0, "y".repeat(200_000)));

        CustomRunResult result = service.runCustom("print(1)", "python", null, constraints);

        assertFalse(result.isSuccess());
        assertTrue(result.getText().startsWith("Output limit exceeded"));
        assertEquals(List.of(""), sandbox.requests.get(0).getInputs());
    }
}
