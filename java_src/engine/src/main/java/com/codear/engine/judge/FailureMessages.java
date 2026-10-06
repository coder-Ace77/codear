package com.codear.engine.judge;

import static com.codear.engine.constants.JudgeDefaults.COMPILE_DIR_PREFIX;
import static com.codear.engine.constants.JudgeDefaults.MAX_COMPILE_MESSAGE_CHARS;
import static com.codear.engine.constants.JudgeDefaults.MAX_DIFF_FIELD_CHARS;
import static com.codear.engine.constants.JudgeDefaults.MAX_STDERR_MESSAGE_CHARS;

import java.util.Map;

/**
 * The sentences a person reads when a submission fails. Everything is capped in length, hides the sandbox's
 * file paths, and only reveals expected output or program errors for tests marked as visible samples.
 */
public final class FailureMessages {

    private FailureMessages() {
    }

    private static final String TRUNCATED = "\n... (truncated)";

    private static final Map<Integer, String> EXIT_REASONS = Map.of(
            132, "illegal instruction",
            134, "aborted (an uncaught exception or a failed assertion)",
            135, "bus error",
            136, "arithmetic exception (for example division by zero)",
            139, "segmentation fault (invalid memory access or stack overflow)");

    public static String compileError(String compilerOutput) {
        String cleaned = clean(compilerOutput, MAX_COMPILE_MESSAGE_CHARS);
        return cleaned.isEmpty() ? "Compilation failed." : "Compilation failed:\n" + cleaned;
    }

    public static String compileTimeout(int seconds) {
        return "Compilation took longer than " + seconds + " seconds.";
    }

    public static String emptySource() {
        return "The source code is empty.";
    }

    public static String sourceTooLarge(int maxChars) {
        return "The source code is too long (the limit is " + maxChars + " characters).";
    }

    public static String unsupportedLanguage(String language) {
        return "Unsupported language: " + language + ".";
    }

    public static String wrongAnswer(TestLocation where, boolean visible, String expected, String actual) {
        String head = "Wrong answer" + where.phrase();
        if (!visible) {
            return head + " (hidden test).";
        }
        return head + ".\nExpected: " + snippet(expected) + "\nGot: " + snippet(actual);
    }

    public static String runtimeError(TestLocation where, boolean visible, int exitCode, String stderr) {
        String reason = EXIT_REASONS.getOrDefault(exitCode, "the program exited with code " + exitCode);
        String head = "Runtime error" + where.phrase() + ": " + reason + ".";
        String detail = visible ? clean(stderr, MAX_STDERR_MESSAGE_CHARS) : "";
        return detail.isEmpty() ? head : head + "\n" + detail;
    }

    public static String timeLimit(TestLocation where, long limitMs) {
        return "Time limit exceeded" + where.phrase() + " (limit " + limitMs + " ms).";
    }

    public static String memoryLimit(TestLocation where, int limitMb) {
        return "Memory limit exceeded" + where.phrase() + " (limit " + limitMb + " MB).";
    }

    public static String outputLimit(TestLocation where) {
        return "Output limit exceeded" + where.phrase() + ": the program printed far more than any correct answer.";
    }

    public static String didNotFinish(TestLocation where) {
        return "The program did not finish" + where.phrase() + ".";
    }

    public static String noTests() {
        return "This problem has no test cases yet, so it cannot be judged.";
    }

    public static String judgeFailure() {
        return "The judge hit an internal error while checking this submission. Please submit again.";
    }

    /** First part of a long output, on one block, so the message stays readable and small. */
    static String snippet(String value) {
        if (value == null || value.isBlank()) {
            return "(no output)";
        }
        String trimmed = value.strip();
        return trimmed.length() <= MAX_DIFF_FIELD_CHARS ? trimmed : trimmed.substring(0, MAX_DIFF_FIELD_CHARS) + "...";
    }

    /** Hides sandbox paths, drops trailing blank space and caps the length. */
    static String clean(String raw, int maxChars) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace(COMPILE_DIR_PREFIX, "")
                .replace("\r", "")
                .stripTrailing();
        if (text.length() > maxChars) {
            return text.substring(0, maxChars).stripTrailing() + TRUNCATED;
        }
        return text;
    }
}
