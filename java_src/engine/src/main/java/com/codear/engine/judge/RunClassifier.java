package com.codear.engine.judge;

import static com.codear.engine.constants.JudgeDefaults.LIMIT_HIT_FRACTION;
import static com.codear.engine.constants.JudgeDefaults.MEMORY_ERROR_MARKERS;

import java.util.Locale;

import com.codear.engine.constants.SandboxProtocol;
import com.codear.engine.sandbox.RunOutcome;
import com.codear.engine.sandbox.TestRun;

/**
 * Decides which limit a run hit, from what the runner recorded. The runner stops the program at its limits and
 * the kernel reports the reason as an exit code, so this is a pure table of rules with no side effects.
 */
public final class RunClassifier {

    public RunOutcome classify(TestRun run, JudgeLimits limits) {
        int exit = run.exitCode();

        if (exit == SandboxProtocol.EXIT_OK) {
            return classifyFinishedRun(run, limits);
        }
        if (exit == SandboxProtocol.EXIT_WALL_TIMEOUT || exit == SandboxProtocol.EXIT_CPU_LIMIT) {
            return RunOutcome.TIME_LIMIT;
        }
        if (exit == SandboxProtocol.EXIT_OUTPUT_LIMIT || hitOutputLimit(run, limits)) {
            return RunOutcome.OUTPUT_LIMIT;
        }
        if (exit == SandboxProtocol.EXIT_KILLED) {
            // SIGKILL comes from three places: the CPU limit, the wall limit and the memory killer.
            return killedByTime(run, limits) ? RunOutcome.TIME_LIMIT : RunOutcome.MEMORY_LIMIT;
        }
        if (mentionsMemoryError(run.stderrTail()) || run.peakKb() > limits.memoryLimitKb()) {
            return RunOutcome.MEMORY_LIMIT;
        }
        return RunOutcome.RUNTIME_ERROR;
    }

    /** A program that exits cleanly can still have used too much time, memory or output. */
    private RunOutcome classifyFinishedRun(TestRun run, JudgeLimits limits) {
        if (hitOutputLimit(run, limits)) {
            return RunOutcome.OUTPUT_LIMIT;
        }
        if (run.cpuMs() > limits.timeLimitMs()) {
            return RunOutcome.TIME_LIMIT;
        }
        if (run.peakKb() > limits.memoryLimitKb()) {
            return RunOutcome.MEMORY_LIMIT;
        }
        return RunOutcome.OK;
    }

    private boolean hitOutputLimit(TestRun run, JudgeLimits limits) {
        return run.stdoutBytes() >= limits.outputLimitBytes();
    }

    private boolean killedByTime(TestRun run, JudgeLimits limits) {
        boolean usedItsCpuTime = run.cpuMs() >= limits.timeLimitMs() * LIMIT_HIT_FRACTION;
        boolean usedItsWallTime = run.wallMs() >= limits.wallLimitSeconds() * 1000L * LIMIT_HIT_FRACTION;
        return usedItsCpuTime || usedItsWallTime;
    }

    private boolean mentionsMemoryError(String stderr) {
        if (stderr == null || stderr.isEmpty()) {
            return false;
        }
        String lower = stderr.toLowerCase(Locale.ROOT);
        return MEMORY_ERROR_MARKERS.stream().anyMatch(marker -> lower.contains(marker.toLowerCase(Locale.ROOT)));
    }
}
