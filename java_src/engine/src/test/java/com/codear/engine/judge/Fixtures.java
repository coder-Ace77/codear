package com.codear.engine.judge;

import java.util.List;

import com.codear.engine.sandbox.CompileResult;
import com.codear.engine.sandbox.SandboxResult;
import com.codear.engine.sandbox.TestRun;

/** Short builders so a test can say what happened in one line. */
final class Fixtures {

    private Fixtures() {
    }

    static final JudgeLimits LIMITS = new JudgeLimits(1000, 256, 64 * 1024, 20);

    /** A clean run of test `index` that printed `stdout`. */
    static TestRun ok(int index, String stdout) {
        return new TestRun(index, 0, 40, 30, 8_000, stdout, stdout.length(), "");
    }

    static TestRun run(int index, int exit, long wallMs, long cpuMs, long peakKb, String stdout, String stderr) {
        return new TestRun(index, exit, wallMs, cpuMs, peakKb, stdout, stdout.length(), stderr);
    }

    static TestRun exitedWith(int exit, String stderr) {
        return run(0, exit, 50, 30, 8_000, "", stderr);
    }

    static SandboxResult finished(TestRun... runs) {
        return new SandboxResult(CompileResult.succeeded(), List.of(runs), true, false);
    }

    static SandboxResult cutOff(boolean oomKilled, TestRun... runs) {
        return new SandboxResult(CompileResult.succeeded(), List.of(runs), false, oomKilled);
    }

    static SandboxResult compileFailed(int exit, String output) {
        return new SandboxResult(new CompileResult(exit, output), List.of(), true, false);
    }

    static ExpectedTest sample(int number, String expected) {
        return new ExpectedTest(number, "in" + number, expected, true);
    }

    static ExpectedTest hidden(int number, String expected) {
        return new ExpectedTest(number, "in" + number, expected, false);
    }
}
