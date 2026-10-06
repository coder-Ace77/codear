package com.codear.engine.judge;

import static com.codear.engine.constants.JudgeDefaults.CONTAINER_MEMORY_HEADROOM_MB;
import static com.codear.engine.constants.JudgeDefaults.KILL_GRACE_SECONDS;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_STARTUP_GRACE_SECONDS;
import static com.codear.engine.constants.JudgeDefaults.WALL_LIMIT_EXTRA_SECONDS;
import static com.codear.engine.constants.JudgeDefaults.WALL_LIMIT_FACTOR;

/** What one submission may use. */
public record JudgeLimits(long timeLimitMs, int memoryLimitMb, long outputLimitBytes, int compileTimeoutSeconds) {

    /** The kernel enforces CPU time in whole seconds, so this is the limit rounded up. */
    public int cpuLimitSeconds() {
        return (int) Math.ceil(timeLimitMs / 1000.0);
    }

    public int wallLimitSeconds() {
        return cpuLimitSeconds() * WALL_LIMIT_FACTOR + WALL_LIMIT_EXTRA_SECONDS;
    }

    public long memoryLimitBytes() {
        return memoryLimitMb * 1024L * 1024L;
    }

    public long memoryLimitKb() {
        return memoryLimitMb * 1024L;
    }

    public int containerMemoryMb() {
        return memoryLimitMb + CONTAINER_MEMORY_HEADROOM_MB;
    }

    /** The longest the whole container may run for this many tests, even if every per-test limit is hit. */
    public long totalTimeoutSeconds(int tests) {
        long perTest = wallLimitSeconds() + KILL_GRACE_SECONDS;
        return compileTimeoutSeconds + tests * perTest + SANDBOX_STARTUP_GRACE_SECONDS;
    }
}
