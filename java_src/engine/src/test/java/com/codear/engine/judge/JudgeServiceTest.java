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
import com.codear.engine.sandbox.SandboxException;
import com.codear.engine.sandbox.SandboxRequest;
import com.codear.engine.sandbox.SandboxResult;
import com.codear.engine.sandbox.SandboxRunner;
import com.codear.engine.service.TestProgressListener;

class JudgeServiceTest {

    /** A sandbox that returns a canned result and remembers what it was asked to run. */
    private static final class FakeSandbox implements SandboxRunner {
        final List<SandboxRequest> requests = new ArrayList<>();
        SandboxResult result;
        RuntimeException failure;

        @Override
        public SandboxResult run(SandboxRequest request, TestProgressListener progress) {
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

        JudgeReport report = service.judge(request("print(1)", "python", tests), null);

        assertEquals(Verdict.ACCEPTED, report.verdict());
        SandboxRequest sent = sandbox.requests.get(0);
        assertEquals(List.of("in1", "in2"), sent.inputs());
        assertEquals(2000, sent.limits().timeLimitMs());
        assertEquals(128, sent.limits().memoryLimitMb());
    }

    @Test
    void emptySourceNeverStartsASandbox() {
        for (String code : new String[] {"", "   \n\t", null}) {
            JudgeReport report = service.judge(request(code, "python", tests), null);

            assertEquals(Verdict.COMPILE_ERROR, report.verdict());
            assertEquals("The source code is empty.", report.message());
        }
        assertTrue(sandbox.requests.isEmpty());
    }

    @Test
    void oversizedSourceIsRejected() {
        JudgeReport report = service.judge(request("x".repeat(70_000), "python", tests), null);

        assertEquals(Verdict.COMPILE_ERROR, report.verdict());
        assertTrue(report.message().contains("too long"));
        assertTrue(sandbox.requests.isEmpty());
    }

    @Test
    void anUnsupportedLanguageIsExplained() {
        JudgeReport report = service.judge(request("code", "brainfuck", tests), null);

        assertEquals(Verdict.COMPILE_ERROR, report.verdict());
        assertEquals("Unsupported language: brainfuck.", report.message());
        assertTrue(sandbox.requests.isEmpty());
    }

    @Test
    void aProblemWithoutTestsIsAJudgeErrorNeverAnAccept() {
        JudgeReport report = service.judge(request("print(1)", "python", List.of()), null);

        assertEquals(Verdict.SYSTEM_ERROR, report.verdict());
        assertFalse(report.verdict().isAccepted());
        assertTrue(sandbox.requests.isEmpty());
    }

    @Test
    void aSandboxFailureBecomesASystemErrorReportNotAnException() {
        sandbox.failure = new SandboxException("docker is down");

        JudgeReport report = service.judge(request("print(1)", "python", tests), null);

        assertEquals(Verdict.SYSTEM_ERROR, report.verdict());
        assertFalse(report.message().contains("docker"), "internal details must not reach the person");
    }

    @Test
    void theProgressListenerIsToldWhenJudgingStarts() {
        sandbox.result = finished(ok(0, "1"), ok(1, "2"));
        boolean[] judging = {false};
        TestProgressListener listener = new TestProgressListener() {
            @Override
            public void judging() {
                judging[0] = true;
            }
        };

        service.judge(request("print(1)", "python", tests), listener);

        assertTrue(judging[0]);
    }

    @Test
    void aCustomRunReturnsTheOutputOfAProgramThatRanCleanly() {
        sandbox.result = finished(ok(0, "hello\n"));

        CustomRunResult result = service.runCustom("print('hello')", "python", "", constraints);

        assertTrue(result.success());
        assertEquals("hello\n", result.text());
    }

    @Test
    void aCustomRunExplainsEachWayItCanFail() {
        sandbox.result = Fixtures.compileFailed(1, "/tmp/build/code.cpp:1:1: error: stray");
        assertEquals("Compilation failed:\ncode.cpp:1:1: error: stray",
                service.runCustom("x", "cpp", "", constraints).text());

        sandbox.result = finished(run(0, 139, 80, 20, 4_000, "", ""));
        assertTrue(service.runCustom("x", "cpp", "", constraints).text().startsWith("Runtime error: segmentation fault"));

        sandbox.result = finished(run(0, 124, 3000, 10, 4_000, "", ""));
        assertEquals("Time limit exceeded (limit 2000 ms).", service.runCustom("x", "cpp", "", constraints).text());

        sandbox.result = finished();
        assertEquals("The program did not finish.", service.runCustom("x", "cpp", "", constraints).text());
    }

    @Test
    void aCustomRunWithEmptyCodeOrABrokenSandboxIsExplained() {
        assertEquals("The source code is empty.", service.runCustom(" ", "python", "", constraints).text());

        sandbox.failure = new SandboxException("boom");
        CustomRunResult result = service.runCustom("print(1)", "python", "", constraints);
        assertFalse(result.success());
        assertFalse(result.text().contains("boom"));
    }

    @Test
    void aCustomRunThatPrintsMoreThanTheOutputCapIsAFloodAndNullInputIsAnEmptyInput() {
        sandbox.result = finished(ok(0, "y".repeat(200_000)));

        CustomRunResult result = service.runCustom("print(1)", "python", null, constraints);

        assertFalse(result.success());
        assertTrue(result.text().startsWith("Output limit exceeded"));
        assertEquals(List.of(""), sandbox.requests.get(0).inputs());
    }
}
