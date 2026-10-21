package com.codear.engine.util.sandbox;

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
import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.dto.sandbox.SandboxRequest;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.HostConfig;

public final class ContainerSpec {

    public List<String> environment(SandboxRequest request) {
        JudgeLimits limits = request.getLimits();
        return List.of(
                SandboxProtocol.ENV_LANGUAGE + "=" + request.getLanguage().id(),
                SandboxProtocol.ENV_NUM_TESTS + "=" + request.getInputs().size(),
                SandboxProtocol.ENV_TIME_LIMIT_MS + "=" + limits.getTimeLimitMs(),
                SandboxProtocol.ENV_CPU_LIMIT_S + "=" + limits.cpuLimitSeconds(),
                SandboxProtocol.ENV_WALL_LIMIT_S + "=" + limits.wallLimitSeconds(),
                SandboxProtocol.ENV_MEMORY_LIMIT_BYTES + "=" + limits.memoryLimitBytes(),
                SandboxProtocol.ENV_OUTPUT_LIMIT_BYTES + "=" + limits.getOutputLimitBytes(),
                SandboxProtocol.ENV_COMPILE_TIMEOUT_S + "=" + limits.getCompileTimeoutSeconds(),
                SandboxProtocol.ENV_RUN_AS + "=" + SANDBOX_RUN_AS);
    }

    public HostConfig hostConfig(JudgeLimits limits) {
        long memory = limits.containerMemoryMb() * 1024L * 1024L;
        return HostConfig.newHostConfig()
                .withNetworkMode("none")
                .withPidsLimit(SANDBOX_PIDS_LIMIT)
                .withNanoCPUs(SANDBOX_NANO_CPUS)
                .withMemory(memory)
                .withMemorySwap(memory) 
                .withCapDrop(Capability.ALL)
                .withCapAdd(Capability.SETUID, Capability.SETGID, Capability.DAC_OVERRIDE, Capability.CHOWN,
                        Capability.FOWNER)
                .withSecurityOpts(List.of("no-new-privileges"))
                .withTmpFs(Map.of(SANDBOX_TMPFS_PATH, SANDBOX_TMPFS_OPTIONS));
    }

    public Map<String, String> labels(long createdAtMillis) {
        return Map.of(SANDBOX_LABEL, "true", SANDBOX_CREATED_LABEL, Long.toString(createdAtMillis));
    }
}
