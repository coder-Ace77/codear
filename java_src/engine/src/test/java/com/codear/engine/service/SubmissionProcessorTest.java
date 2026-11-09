package com.codear.engine.service;

import static com.codear.engine.judge.Fixtures.finished;
import static com.codear.engine.judge.Fixtures.hidden;
import static com.codear.engine.judge.Fixtures.ok;
import static com.codear.engine.judge.Fixtures.sample;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.judge.RunReport;
import com.codear.engine.dto.judge.ExpectedTest;
import com.codear.engine.dto.judge.JudgeReport;
import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.dto.sandbox.SandboxResult;
import com.codear.engine.enums.Language;
import com.codear.engine.enums.Verdict;
import com.codear.engine.exception.SandboxException;
import com.codear.engine.judge.JudgeService;
import com.codear.engine.sandbox.SandboxRunner;

class SubmissionProcessorTest {

    private static final class FakeSandbox implements SandboxRunner {
        int calls;
        List<String> inputs;
        JudgeLimits limits;
        SandboxResult result;
        RuntimeException failure;

        @Override
        public SandboxResult run(Language language, String code, List<String> inputs, JudgeLimits limits) {
            this.calls++;
            this.inputs = inputs;
            this.limits = limits;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private final FakeSandbox sandbox = new FakeSandbox();
    private final SubmissionProcessor processor = new SubmissionProcessor(
            mock(ProblemCrudService.class), mock(SubmissionService.class), sandbox, new JudgeService());
    private final ResourceConstraints constraints = new ResourceConstraints(2000L, 128, "Medium");
    private final List<ExpectedTest> tests = List.of(sample(1, "1"), hidden(2, "2"));

    @Test
    void aGoodSubmissionIsRunWithTheProblemsLimitsAndAccepted() {
        sandbox.result = finished(ok(0, "1"), ok(1, "2"));

        JudgeReport report = processor.run("print(1)", "python", tests, constraints);

        assertEquals(Verdict.ACCEPTED, report.getVerdict());
        assertEquals(List.of("in1", "in2"), sandbox.inputs);
        assertEquals(2000, sandbox.limits.getTimeLimitMs());
        assertEquals(128, sandbox.limits.getMemoryLimitMb());
    }

    @Test
    void emptySourceNeverStartsASandbox() {
        for (String code : new String[] {"", "   \n\t", null}) {
            JudgeReport report = processor.run(code, "python", tests, constraints);

            assertEquals(Verdict.COMPILE_ERROR, report.getVerdict());
            assertEquals("The source code is empty.", report.getMessage());
        }
        assertEquals(0, sandbox.calls);
    }

    @Test
    void oversizedSourceIsRejected() {
        JudgeReport report = processor.run("x".repeat(70_000), "python", tests, constraints);

        assertEquals(Verdict.COMPILE_ERROR, report.getVerdict());
        assertTrue(report.getMessage().contains("too long"));
        assertEquals(0, sandbox.calls);
    }

    @Test
    void anUnsupportedLanguageIsExplained() {
        JudgeReport report = processor.run("code", "brainfuck", tests, constraints);

        assertEquals(Verdict.COMPILE_ERROR, report.getVerdict());
        assertEquals("Unsupported language: brainfuck.", report.getMessage());
        assertEquals(0, sandbox.calls);
    }

    @Test
    void aSandboxFailureBecomesASystemErrorReportNotAnException() {
        sandbox.failure = new SandboxException("docker is down");

        JudgeReport report = processor.run("print(1)", "python", tests, constraints);

        assertEquals(Verdict.SYSTEM_ERROR, report.getVerdict());
        assertFalse(report.getMessage().contains("docker"), "internal details must not reach the person");
    }

    @Test
    void aCustomRunWithEmptyCodeOrABrokenSandboxIsExplained() {
        assertEquals("The source code is empty.",
                processor.run(" ", "python", "", constraints).getText());

        sandbox.failure = new SandboxException("boom");
        RunReport result = processor.run("print(1)", "python", "", constraints);
        assertFalse(result.isSuccess());
        assertFalse(result.getText().contains("boom"));
    }

    @Test
    void aCustomRunWithNoInputIsGivenAnEmptyInput() {
        sandbox.result = finished(ok(0, "hi"));

        RunReport result = processor.run("print('hi')", "python", (String) null, constraints);

        assertTrue(result.isSuccess());
        assertEquals(List.of(""), sandbox.inputs);
    }
}
