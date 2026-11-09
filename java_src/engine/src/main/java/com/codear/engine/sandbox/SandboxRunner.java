package com.codear.engine.sandbox;

import java.util.List;

import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.dto.sandbox.SandboxResult;
import com.codear.engine.enums.Language;

public interface SandboxRunner {

    SandboxResult run(Language language, String code, List<String> inputs, JudgeLimits limits);
}
