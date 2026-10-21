package com.codear.engine.enums;

import com.codear.engine.constants.SandboxProtocol;

public enum CompileStatus {
    OK,
    FAILED,
    TIMED_OUT;

    public static CompileStatus fromExitCode(int exitCode) {
        if (exitCode == SandboxProtocol.EXIT_OK) {
            return OK;
        }
        if (exitCode == SandboxProtocol.EXIT_WALL_TIMEOUT || exitCode == SandboxProtocol.EXIT_KILLED) {
            return TIMED_OUT;
        }
        return FAILED;
    }
}
