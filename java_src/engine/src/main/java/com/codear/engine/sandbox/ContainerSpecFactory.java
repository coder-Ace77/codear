package com.codear.engine.sandbox;

import static com.codear.engine.constants.JudgeDefaults.SANDBOX_CREATED_LABEL;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_LABEL;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_NANO_CPUS;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_PIDS_LIMIT;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_RUN_AS;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_TMPFS_OPTIONS;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_TMPFS_PATH;

import java.util.List;
import java.util.Map;

import com.codear.engine.constants.SandboxProtocol;
import com.codear.engine.judge.JudgeLimits;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.HostConfig;

/**
 * What the sandbox container looks like: the environment the runner reads and the restrictions Docker enforces.
 * The restrictions are the outer wall; the runner adds its own per-test limits inside.
 */
public final class ContainerSpecFactory {

    public List<String> environment(SandboxRequest request) {
        JudgeLimits limits = request.limits();
        return List.of(
                SandboxProtocol.ENV_LANGUAGE + "=" + request.language().id(),
                SandboxProtocol.ENV_NUM_TESTS + "=" + request.inputs().size(),
                SandboxProtocol.ENV_TIME_LIMIT_MS + "=" + limits.timeLimitMs(),
                SandboxProtocol.ENV_CPU_LIMIT_S + "=" + limits.cpuLimitSeconds(),
                SandboxProtocol.ENV_WALL_LIMIT_S + "=" + limits.wallLimitSeconds(),
                SandboxProtocol.ENV_MEMORY_LIMIT_BYTES + "=" + limits.memoryLimitBytes(),
                SandboxProtocol.ENV_OUTPUT_LIMIT_BYTES + "=" + limits.outputLimitBytes(),
                SandboxProtocol.ENV_COMPILE_TIMEOUT_S + "=" + limits.compileTimeoutSeconds(),
                SandboxProtocol.ENV_RUN_AS + "=" + SANDBOX_RUN_AS,
                SandboxProtocol.ENV_TEST_SEPARATOR + "=" + SandboxProtocol.TEST_SEPARATOR);
    }

    public HostConfig hostConfig(JudgeLimits limits) {
        long memory = limits.containerMemoryMb() * 1024L * 1024L;
        return HostConfig.newHostConfig()
                .withNetworkMode("none")
                .withPidsLimit(SANDBOX_PIDS_LIMIT)
                .withNanoCPUs(SANDBOX_NANO_CPUS)
                .withMemory(memory)
                .withMemorySwap(memory) // equal to memory: no swap, so a memory hog is killed, not slowed
                .withCapDrop(Capability.ALL)
                // only what the runner needs to switch to the unprivileged user and to manage its own files
                .withCapAdd(Capability.SETUID, Capability.SETGID, Capability.DAC_OVERRIDE, Capability.CHOWN,
                        Capability.FOWNER)
                .withSecurityOpts(List.of("no-new-privileges"))
                .withTmpFs(Map.of(SANDBOX_TMPFS_PATH, SANDBOX_TMPFS_OPTIONS));
    }

    /** Marks the container as ours and records when, so a janitor can find the ones a crash left behind. */
    public Map<String, String> labels(long createdAtMillis) {
        return Map.of(SANDBOX_LABEL, "true", SANDBOX_CREATED_LABEL, Long.toString(createdAtMillis));
    }
}
