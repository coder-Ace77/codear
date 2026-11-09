package com.codear.engine.judge;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.codear.engine.checker.OutputComparator;
import com.codear.engine.dto.judge.RunReport;
import com.codear.engine.dto.judge.ExpectedTest;
import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.dto.judge.JudgeReport;
import com.codear.engine.dto.judge.TestLocation;
import com.codear.engine.dto.sandbox.SandboxResult;
import com.codear.engine.dto.sandbox.SandboxResult.TestRun;
import com.codear.engine.enums.CompileStatus;
import com.codear.engine.enums.RunOutcome;
import com.codear.engine.util.judge.FailureMessages;

@Service
public class JudgeService {

    private final RunClassifier classifier = new RunClassifier();

    public JudgeReport judge(SandboxResult result, List<ExpectedTest> tests, JudgeLimits limits,
            OutputComparator checker) {
        return new VerdictResolver(checker).resolve(result, tests, limits);
    }

    public RunReport judgeCustomRun(SandboxResult result, JudgeLimits limits) {
        if (result.getCompile().getStatus() != CompileStatus.OK) {
            String message = result.getCompile().getStatus() == CompileStatus.TIMED_OUT
                    ? FailureMessages.compileTimeout(limits.getCompileTimeoutSeconds())
                    : FailureMessages.compileError(result.getCompile().getOutput());
            return new RunReport(message, false);
        }
        Optional<TestRun> recorded = result.getRunFromIndex(0);
        if (recorded.isEmpty()) {
            return new RunReport(FailureMessages.didNotFinish(TestLocation.customRun()), false);
        }
        TestRun run = recorded.get();
        TestLocation where = TestLocation.customRun();
        RunOutcome outcome = classifier.classify(run, limits);
        return switch (outcome) {
            case OK -> new RunReport(run.getStdout(), true);
            case TIME_LIMIT -> new RunReport(FailureMessages.timeLimit(where, limits.getTimeLimitMs()), false);
            case MEMORY_LIMIT -> new RunReport(FailureMessages.memoryLimit(where, limits.getMemoryLimitMb()), false);
            case OUTPUT_LIMIT -> new RunReport(FailureMessages.outputLimit(where), false);
            case RUNTIME_ERROR -> new RunReport(
                    FailureMessages.runtimeError(where, true, run.getExitCode(), run.getStderrTail()), false);
        };
    }
}
