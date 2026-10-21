package com.codear.engine.dto.judge;

import java.util.List;

import com.codear.engine.checker.OutputComparator;
import com.codear.engine.dto.ResourceConstraints;

import lombok.Value;

@Value
public class JudgeRequest {

    String code;
    String language;
    List<ExpectedTest> tests;
    ResourceConstraints constraints;
    OutputComparator comparator;
}
