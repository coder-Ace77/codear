package com.codear.engine.service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.entity.TestCase;
import com.codear.engine.enums.RunStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class CheckerServiceTest {

    private final CheckerService checkerService = new CheckerService();

    private TestCase testCase(String expected) {
        TestCase testCase = new TestCase();
        testCase.setInput("Sample Input");
        testCase.setOutput(expected);
        testCase.setIsHidden(false);
        return testCase;
    }

    @Test
    void testCheckWithSameLineMarker() {
        CheckerService checkerService = new CheckerService();
        TestCase testCase = new TestCase();
        testCase.setInput("Sample Input");
        testCase.setOutput("20");
        testCase.setId(1L);

        List<String> userOutputs = Collections.singletonList("20[TEST-OUTPUT-END]");
        List<TestCase> testCases = Collections.singletonList(testCase);

        CheckerResponse response = checkerService.check(userOutputs, testCases);

        assertEquals(RunStatus.PASSED, response.getStatus(), "Status should be PASSED even with marker on same line");
        assertEquals("All tests passed", response.getMsg());
    }

    @Test
    void testCheckNormal() {
        CheckerService checkerService = new CheckerService();
        TestCase testCase = new TestCase();
        testCase.setInput("Sample Input");
        testCase.setOutput("20");

        List<String> userOutputs = Collections.singletonList("20");
        List<TestCase> testCases = Collections.singletonList(testCase);

        CheckerResponse response = checkerService.check(userOutputs, testCases);

        assertEquals(RunStatus.PASSED, response.getStatus());
    }

    @Test
    void wrongAnswerFailsAndReportsTheExpectedOutput() {
        CheckerResponse response = checkerService.check(
                Collections.singletonList("21"),
                Collections.singletonList(testCase("20")));

        assertEquals(RunStatus.FAILED, response.getStatus());
        assertEquals(1, response.getTotalTests());
        assertEquals(0, response.getPassedTests());
        assertTrue(response.getMsg().contains("EXPECTED: 20"), response.getMsg());
    }

    @Test
    void hiddenTestCasesDoNotLeakTheirOutput() {
        TestCase hidden = testCase("20");
        hidden.setIsHidden(true);

        CheckerResponse response = checkerService.check(
                Collections.singletonList("21"), Collections.singletonList(hidden));

        assertEquals(RunStatus.FAILED, response.getStatus());
        assertEquals("Test Case 1 Failed: Hidden Test Case", response.getMsg());
    }

    @Test
    void onlyTheFirstFailureIsReported() {
        CheckerResponse response = checkerService.check(
                Arrays.asList("1", "wrong", "wrong"),
                Arrays.asList(testCase("1"), testCase("2"), testCase("3")));

        assertEquals(RunStatus.FAILED, response.getStatus());
        assertEquals(3, response.getTotalTests());
        assertEquals(1, response.getPassedTests());
        assertTrue(response.getMsg().startsWith("Test Case 2 Failed"), response.getMsg());
    }

    @Test
    void missingOutputCountsAsAFailure() {
        CheckerResponse response = checkerService.check(
                Collections.singletonList("1"),
                Arrays.asList(testCase("1"), testCase("2")));

        assertEquals(RunStatus.FAILED, response.getStatus());
        assertEquals(1, response.getPassedTests());
    }

    @Test
    void surroundingWhitespaceAndBlankLinesAreIgnored() {
        CheckerResponse response = checkerService.check(
                Collections.singletonList("  20  \n\n"),
                Collections.singletonList(testCase("20")));

        assertEquals(RunStatus.PASSED, response.getStatus());
    }

    @Test
    void markerLinesAreStrippedFromMultiLineOutput() {
        CheckerResponse response = checkerService.check(
                Collections.singletonList("1\n2\n[TEST-OUTPUT-END]"),
                Collections.singletonList(testCase("1\n2")));

        assertEquals(RunStatus.PASSED, response.getStatus());
    }

    @Test
    void nullOutputFailsAgainstANonEmptyExpectation() {
        CheckerResponse response = checkerService.check(
                Collections.singletonList(null),
                Collections.singletonList(testCase("20")));

        assertEquals(RunStatus.FAILED, response.getStatus());
        assertEquals(0, response.getPassedTests());
    }
}
