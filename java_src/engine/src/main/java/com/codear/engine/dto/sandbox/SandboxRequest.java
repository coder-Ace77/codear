package com.codear.engine.dto.sandbox;

import java.util.List;

import com.codear.engine.enums.Language;
import com.codear.engine.judge.JudgeLimits;

import lombok.Value;

@Value
public class SandboxRequest {

    Language language;
    String code;
    List<String> inputs;
    JudgeLimits limits;
}
