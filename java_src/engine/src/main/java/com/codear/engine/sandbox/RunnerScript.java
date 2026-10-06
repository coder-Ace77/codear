package com.codear.engine.sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.codear.engine.constants.SandboxProtocol;

/** The runner script that executes inside the sandbox, read once from the classpath. */
public final class RunnerScript {

    private static final String CONTENTS = load();

    private RunnerScript() {
    }

    public static String contents() {
        return CONTENTS;
    }

    private static String load() {
        try (InputStream in = RunnerScript.class.getClassLoader().getResourceAsStream(SandboxProtocol.RUNNER_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + SandboxProtocol.RUNNER_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
