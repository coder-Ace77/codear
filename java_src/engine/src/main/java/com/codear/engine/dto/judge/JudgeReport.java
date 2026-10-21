package com.codear.engine.dto.judge;

import com.codear.engine.enums.Verdict;

import lombok.Value;

/**
 * The outcome of judging one submission.
 */
@Value
public class JudgeReport {

    Verdict verdict;
    /** 1-based number of the first test that failed; null when none did (accepted, compile error) */
    Integer failedTest;
    int totalTests;
    int passedTests;
    /** what the person sees */
    String message;
    /** extra detail kept with the submission (not shown by default) */
    String diagnostics;
    long maxTimeMs;
    long peakMemoryKb;

    public static final String ALL_PASSED = "All tests passed";

    public static JudgeReport accepted(int totalTests, long maxTimeMs, long peakMemoryKb) {
        return new JudgeReport(Verdict.ACCEPTED, null, totalTests, totalTests, ALL_PASSED, "", maxTimeMs, peakMemoryKb);
    }

    public static JudgeReport failedOnTest(Verdict verdict, int failedTest, int totalTests, String message,
            String diagnostics, long maxTimeMs, long peakMemoryKb) {
        return new JudgeReport(verdict, failedTest, totalTests, failedTest - 1, message, diagnostics, maxTimeMs,
                peakMemoryKb);
    }

    public static JudgeReport compileError(int totalTests, String message, String diagnostics) {
        return new JudgeReport(Verdict.COMPILE_ERROR, null, totalTests, 0, message, diagnostics, 0, 0);
    }

    public static JudgeReport systemError(int totalTests, String message) {
        return new JudgeReport(Verdict.SYSTEM_ERROR, null, totalTests, 0, message, "", 0, 0);
    }

    /** "3.34MB", or "0MB" when nothing was measured. */
    public String memoryLabel() {
        return peakMemoryKb <= 0 ? "0MB" : String.format("%.2fMB", peakMemoryKb / 1024.0);
    }
}
