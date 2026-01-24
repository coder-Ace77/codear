package com.codear.engine.service;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.CheckerResponse;
import com.codear.engine.entity.TestCase;
import com.codear.engine.enums.RunStatus;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckerService {

    public CheckerResponse check(List<String> outputs, List<TestCase> tests) {
        int size = tests.size();
        int passedCount = 0;
        String firstFailureDetail = null;
        for (int index = 0; index < size; index++) {
            String actualRaw = (index < outputs.size()) ? outputs.get(index) : "";
            String expectedRaw = tests.get(index).getOutput();

            String actualNormalized = normalize(actualRaw);
            String expectedNormalized = normalize(expectedRaw);

            if (actualNormalized.equals(expectedNormalized)) {
                passedCount++;
            } else if (firstFailureDetail == null) {
                TestCase failedTest = tests.get(index);
                if (Boolean.TRUE.equals(failedTest.getIsHidden())) {
                    firstFailureDetail = String.format("Test Case %d Failed: Hidden Test Case", index + 1);
                } else {
                    firstFailureDetail = String.format("Test Case %d Failed:\n OUTPUT: %s \n EXPECTED: %s", index + 1,
                            actualNormalized, expectedNormalized);
                }
            }
        }
        if (passedCount == size) {
            return new CheckerResponse(RunStatus.PASSED, "All tests passed", size, passedCount);
        } else {
            return new CheckerResponse(RunStatus.FAILED, firstFailureDetail, size, passedCount);
        }
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.lines()
                .map(line -> line.replace("[TEST-OUTPUT-END]", "")) 
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("[TEST-"))
                .collect(Collectors.joining("\n"));
    }
}