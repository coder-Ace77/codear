package com.codear.engine.service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.entity.TestCase;
import com.codear.engine.enums.RunStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

class CheckerServiceTest {

    @Test
    void testCheckWithSameLineMarker() {
        CheckerService checkerService = new CheckerService();
        TestCase testCase = new TestCase();
        testCase.setInput("Sample Input");
        testCase.setOutput("20");
        testCase.setId(1L);

        // Simulate output with marker on same line
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
}
