package com.codear.engine.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.codear.engine.judge.JudgeLimits;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.HostConfig;

/** The small pieces that set a sandbox up: language lookup, the scratch folder, and the container's restrictions. */
class SandboxSetupTest {

    @Test
    void languagesAreFoundByNameIgnoringCaseAndSpaces() {
        assertEquals(Language.PYTHON, Language.from("python"));
        assertEquals(Language.CPP, Language.from(" CPP "));
        assertEquals("code.cpp", Language.CPP.sourceFile());
    }

    @Test
    void anUnknownOrMissingLanguageIsRejected() {
        assertThrows(Language.UnsupportedLanguageException.class, () -> Language.from("brainfuck"));
        assertThrows(Language.UnsupportedLanguageException.class, () -> Language.from(null));
    }

    @Test
    void theRunnerScriptIsOnTheClasspathAndSpeaksTheProtocol() {
        String script = RunnerScript.contents();

        assertTrue(script.startsWith("#!/bin/sh"));
        for (String name : List.of("LANGUAGE", "NUM_TESTS", "TIME_LIMIT_MS", "CPU_LIMIT_S", "WALL_LIMIT_S",
                "MEMORY_LIMIT_BYTES", "OUTPUT_LIMIT_BYTES", "COMPILE_TIMEOUT_S", "RUN_AS", "TEST_SEPARATOR")) {
            assertTrue(script.contains(name), "the runner does not read " + name);
        }
        for (String file : List.of("compile.status", "compile.txt", "code_", "meta_", "out_", "err_", "done")) {
            assertTrue(script.contains(file), "the runner does not write " + file);
        }
    }

    @Test
    void aWorkspaceHoldsTheSourceInputsAndRunnerAndDisappearsWhenClosed(@TempDir Path root) throws IOException {
        Path folder;
        try (SandboxWorkspace workspace = SandboxWorkspace.create(root, Language.PYTHON, "print(1)", List.of("a", "b"))) {
            folder = workspace.directory();
            assertTrue(SandboxWorkspace.isWorkspaceName(workspace.folderName()));
            assertEquals("print(1)", Files.readString(folder.resolve("code.py")));
            assertEquals("a", Files.readString(folder.resolve("input_0.txt")));
            assertEquals("b", Files.readString(folder.resolve("input_1.txt")));
            assertTrue(Files.readString(folder.resolve("runner.sh")).startsWith("#!/bin/sh"));
        }
        assertFalse(Files.exists(folder));
    }

    @Test
    void onlyFoldersMadeByTheEngineAreRecognisedAsWorkspaces() {
        assertTrue(SandboxWorkspace.isWorkspaceName("codear_12345"));
        assertFalse(SandboxWorkspace.isWorkspaceName("my-photos"));
        assertFalse(SandboxWorkspace.isWorkspaceName(null));
    }

    @Test
    void theEnvironmentCarriesEveryLimitTheRunnerNeeds() {
        JudgeLimits limits = new JudgeLimits(1500, 128, 200_000, 20);
        List<String> env = new ContainerSpecFactory().environment(
                new SandboxRequest(Language.CPP, "code", List.of("a", "b", "c"), limits));

        assertTrue(env.contains("LANGUAGE=cpp"));
        assertTrue(env.contains("NUM_TESTS=3"));
        assertTrue(env.contains("TIME_LIMIT_MS=1500"));
        assertTrue(env.contains("CPU_LIMIT_S=2"));
        assertTrue(env.contains("WALL_LIMIT_S=5"));
        assertTrue(env.contains("MEMORY_LIMIT_BYTES=" + 128L * 1024 * 1024));
        assertTrue(env.contains("OUTPUT_LIMIT_BYTES=200000"));
        assertTrue(env.contains("COMPILE_TIMEOUT_S=20"));
        assertTrue(env.contains("RUN_AS=65534:65534"));
        assertTrue(env.contains("TEST_SEPARATOR=" + com.codear.engine.constants.SandboxProtocol.TEST_SEPARATOR));
    }

    @Test
    void theContainerHasNoNetworkNoSwapFewProcessesAndFewPrivileges() {
        HostConfig config = new ContainerSpecFactory().hostConfig(new JudgeLimits(1000, 256, 1, 20));

        assertEquals("none", config.getNetworkMode());
        assertEquals(64L, config.getPidsLimit());
        assertEquals(1_000_000_000L, config.getNanoCPUs());
        assertEquals(512L * 1024 * 1024, config.getMemory());
        assertEquals(config.getMemory(), config.getMemorySwap());
        assertEquals(List.of(Capability.ALL), List.of(config.getCapDrop()));
        assertFalse(List.of(config.getCapAdd()).contains(Capability.NET_ADMIN));
        assertFalse(List.of(config.getCapAdd()).contains(Capability.SYS_ADMIN));
        assertTrue(config.getSecurityOpts().contains("no-new-privileges"));
        assertTrue(config.getTmpFs().get("/tmp").contains("size=64m"));
    }

    @Test
    void containersAreLabelledSoAJanitorCanFindTheirs() {
        var labels = new ContainerSpecFactory().labels(1234L);

        assertEquals("true", labels.get("codear.sandbox"));
        assertEquals("1234", labels.get("codear.created"));
    }
}
