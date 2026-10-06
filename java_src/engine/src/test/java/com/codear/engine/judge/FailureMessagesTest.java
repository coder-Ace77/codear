package com.codear.engine.judge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FailureMessagesTest {

    private static final TestLocation THIRD_OF_TWENTY_SIX = new TestLocation(3, 26);

    @Test
    void wrongAnswerOnAVisibleTestShowsExpectedAndGot() {
        String message = FailureMessages.wrongAnswer(THIRD_OF_TWENTY_SIX, true, "YES", "NO");

        assertEquals("Wrong answer on test 3 of 26.\nExpected: YES\nGot: NO", message);
    }

    @Test
    void wrongAnswerOnAHiddenTestRevealsNothingAboutTheOutput() {
        String message = FailureMessages.wrongAnswer(THIRD_OF_TWENTY_SIX, false, "SECRET-ANSWER", "SECRET-OUTPUT");

        assertEquals("Wrong answer on test 3 of 26 (hidden test).", message);
        assertFalse(message.contains("SECRET"));
    }

    @Test
    void longOutputsAreCutDownInAWrongAnswerMessage() {
        String message = FailureMessages.wrongAnswer(THIRD_OF_TWENTY_SIX, true, "a".repeat(5000), "b".repeat(5000));

        assertTrue(message.length() < 600, "message was " + message.length() + " chars");
        assertTrue(message.contains("..."));
    }

    @Test
    void noOutputIsSaidInWords() {
        assertTrue(FailureMessages.wrongAnswer(THIRD_OF_TWENTY_SIX, true, "YES", "  \n").endsWith("Got: (no output)"));
    }

    @Test
    void compilerOutputLosesTheSandboxPathAndIsLabelled() {
        String message = FailureMessages.compileError("/tmp/build/code.cpp:1:20: error: 'x' was not declared");

        assertEquals("Compilation failed:\ncode.cpp:1:20: error: 'x' was not declared", message);
    }

    @Test
    void aHugeCompilerOutputIsCapped() {
        String message = FailureMessages.compileError("error\n".repeat(10_000));

        assertTrue(message.length() < 3_200);
        assertTrue(message.endsWith("(truncated)"));
    }

    @Test
    void anEmptyCompilerOutputStillSaysItFailed() {
        assertEquals("Compilation failed.", FailureMessages.compileError("  \n"));
        assertEquals("Compilation failed.", FailureMessages.compileError(null));
    }

    @Test
    void runtimeErrorNamesTheSignal() {
        assertTrue(FailureMessages.runtimeError(THIRD_OF_TWENTY_SIX, true, 139, "").contains("segmentation fault"));
        assertTrue(FailureMessages.runtimeError(THIRD_OF_TWENTY_SIX, true, 136, "").contains("division by zero"));
        assertTrue(FailureMessages.runtimeError(THIRD_OF_TWENTY_SIX, true, 134, "").contains("uncaught exception"));
        assertTrue(FailureMessages.runtimeError(THIRD_OF_TWENTY_SIX, true, 3, "").contains("exited with code 3"));
    }

    @Test
    void runtimeErrorShowsTheProgramsErrorOutputOnlyForVisibleTests() {
        String stderr = "Traceback (most recent call last):\n  File \"/tmp/build/code.py\", line 2\nValueError: boom";

        String visible = FailureMessages.runtimeError(THIRD_OF_TWENTY_SIX, true, 1, stderr);
        String hidden = FailureMessages.runtimeError(THIRD_OF_TWENTY_SIX, false, 1, stderr);

        assertTrue(visible.contains("ValueError: boom"));
        assertFalse(visible.contains("/tmp/build/"));
        assertFalse(hidden.contains("ValueError"));
    }

    @Test
    void limitMessagesSayWhichTestAndWhatTheLimitWas() {
        assertEquals("Time limit exceeded on test 3 of 26 (limit 2000 ms).", FailureMessages.timeLimit(THIRD_OF_TWENTY_SIX, 2000));
        assertEquals("Memory limit exceeded on test 3 of 26 (limit 256 MB).", FailureMessages.memoryLimit(THIRD_OF_TWENTY_SIX, 256));
        assertTrue(FailureMessages.outputLimit(THIRD_OF_TWENTY_SIX).startsWith("Output limit exceeded on test 3 of 26"));
    }

    @Test
    void aPersonsOwnRunHasNoTestNumber() {
        assertEquals("Time limit exceeded (limit 1000 ms).", FailureMessages.timeLimit(TestLocation.customRun(), 1000));
        assertEquals("Runtime error: segmentation fault (invalid memory access or stack overflow).",
                FailureMessages.runtimeError(TestLocation.customRun(), false, 139, ""));
    }
}
