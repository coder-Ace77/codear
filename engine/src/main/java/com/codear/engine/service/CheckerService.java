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
            String actualRaw = outputs.get(index);
            String expectedRaw = tests.get(index).getOutput();

            String actualNormalized = normalize(actualRaw);
            String expectedNormalized = normalize(expectedRaw);

            if (actualNormalized.equals(expectedNormalized)) {
                passedCount++;
            } else if (firstFailureDetail == null) {
                firstFailureDetail = String.format(
                    "Test Case %d Failed:\n\n--- EXPECTED OUTPUT ---\n%s\n\n--- YOUR OUTPUT ---\n%s",
                    index, 
                    expectedRaw,
                    actualRaw
                );
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
                .map(String::trim)            
                .filter(line -> !line.isEmpty()) 
                .collect(Collectors.joining("\n")); 
    }
}