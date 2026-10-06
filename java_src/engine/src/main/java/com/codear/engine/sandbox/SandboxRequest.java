package com.codear.engine.sandbox;

import java.util.List;

import com.codear.engine.judge.JudgeLimits;

public record SandboxRequest(Language language, String code, List<String> inputs, JudgeLimits limits) {
}
