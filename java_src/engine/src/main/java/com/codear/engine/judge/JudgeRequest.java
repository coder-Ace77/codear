package com.codear.engine.judge;

import java.util.List;

import com.codear.engine.checker.OutputComparator;
import com.codear.engine.dto.ResourceConstraints;

public record JudgeRequest(String code, String language, List<ExpectedTest> tests,
        ResourceConstraints constraints, OutputComparator comparator) {
}
