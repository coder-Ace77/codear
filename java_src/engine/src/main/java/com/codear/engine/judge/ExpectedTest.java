package com.codear.engine.judge;

import java.util.List;
import java.util.stream.IntStream;

import com.codear.engine.entity.TestCase;

import lombok.Value;

/**
 * One test as the judge sees it. `number` counts from 1. Only tests marked as samples may have their expected
 * output shown to the person submitting; everything else is hidden.
 */
@Value
public class ExpectedTest {

    int number;
    String input;
    String expectedOutput;
    boolean visible;

    public static ExpectedTest from(int index, TestCase testCase) {
        return new ExpectedTest(
                index + 1,
                nullToEmpty(testCase.getInput()),
                nullToEmpty(testCase.getOutput()),
                testCase.isSample());
    }

    /** Numbers the stored tests from 1 in the order given. */
    public static List<ExpectedTest> fromAll(List<TestCase> testCases) {
        return IntStream.range(0, testCases.size())
                .mapToObj(i -> from(i, testCases.get(i)))
                .toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
