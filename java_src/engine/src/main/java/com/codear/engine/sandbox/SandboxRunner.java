package com.codear.engine.sandbox;

/** Runs untrusted code against inputs under limits and reports what happened. Docker is one way to do it. */
public interface SandboxRunner {

    /** @throws SandboxException when the sandbox itself fails */
    SandboxResult run(SandboxRequest request);
}
