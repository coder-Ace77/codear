package com.codear.engine.judge;

import com.codear.engine.entity.TestCase;

/**
 * One test as the judge sees it. `number` counts from 1. Only tests marked as samples may have their expected
 * output shown to the person submitting; everything else is hidden.
 */
public record ExpectedTest(int number, String input, String expectedOutput, boolean visible) {

    public static ExpectedTest from(int index, TestCase testCase) {
        return new ExpectedTest(
                index + 1,
                nullToEmpty(testCase.getInput()),
                nullToEmpty(testCase.getOutput()),
                testCase.isSample());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
