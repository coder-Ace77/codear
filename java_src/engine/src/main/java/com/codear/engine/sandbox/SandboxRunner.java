package com.codear.engine.sandbox;

import com.codear.engine.dto.sandbox.SandboxRequest;
import com.codear.engine.dto.sandbox.SandboxResult;

public interface SandboxRunner {

    SandboxResult run(SandboxRequest request);
}
