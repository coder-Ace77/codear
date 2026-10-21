package com.codear.engine.dto.sandbox;

import java.util.List;

import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.enums.Language;

import lombok.Value;

@Value
public class SandboxRequest {

    Language language;
    String code;
    List<String> inputs;
    JudgeLimits limits;
}
