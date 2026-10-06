package com.codear.engine.sandbox;

import com.codear.engine.constants.SandboxProtocol;

public record CompileResult(int exitCode, String output) {

    public boolean ok() {
        return exitCode == SandboxProtocol.EXIT_OK;
    }

    public boolean timedOut() {
        return exitCode == SandboxProtocol.EXIT_WALL_TIMEOUT || exitCode == SandboxProtocol.EXIT_KILLED;
    }

    public static CompileResult succeeded() {
        return new CompileResult(SandboxProtocol.EXIT_OK, "");
    }
}
