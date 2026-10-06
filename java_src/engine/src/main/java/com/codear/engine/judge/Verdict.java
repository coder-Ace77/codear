package com.codear.engine.judge;

import com.codear.engine.enums.RunStatus;

/** Why a submission got the result it got. {@link RunStatus} stays the coarse PASSED/FAILED the rest of the system reads. */
public enum Verdict {
    ACCEPTED("Accepted", RunStatus.PASSED),
    WRONG_ANSWER("Wrong answer", RunStatus.FAILED),
    COMPILE_ERROR("Compile error", RunStatus.FAILED),
    RUNTIME_ERROR("Runtime error", RunStatus.FAILED),
    TIME_LIMIT_EXCEEDED("Time limit exceeded", RunStatus.FAILED),
    MEMORY_LIMIT_EXCEEDED("Memory limit exceeded", RunStatus.FAILED),
    OUTPUT_LIMIT_EXCEEDED("Output limit exceeded", RunStatus.FAILED),
    /** The judge failed, not the submission. */
    SYSTEM_ERROR("System error", RunStatus.FAILED);

    private final String label;
    private final RunStatus runStatus;

    Verdict(String label, RunStatus runStatus) {
        this.label = label;
        this.runStatus = runStatus;
    }

    public String label() {
        return label;
    }

    public RunStatus runStatus() {
        return runStatus;
    }

    public boolean isAccepted() {
        return this == ACCEPTED;
    }
}
