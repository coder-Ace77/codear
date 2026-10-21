package com.codear.engine.dto.sandbox;

import java.util.List;
import java.util.Optional;

import com.codear.engine.enums.CompileStatus;

import lombok.Value;

@Value
public class SandboxResult {

    CompileResult compile;
    List<TestRun> runs;
    boolean finished;
    boolean oomKilled;

    public Optional<TestRun> runFor(int index) {
        return runs.stream().filter(run -> run.getIndex() == index).findFirst();
    }

    @Value
    public static class CompileResult {

        CompileStatus status;
        String output;
    }

    @Value
    public static class TestRun {

        int index;
        int exitCode;
        long wallMs;
        long cpuMs;
        long peakKb;
        String stdout;
        long stdoutBytes;
        String stderrTail;
    }
}
