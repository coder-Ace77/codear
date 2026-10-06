package com.codear.engine.sandbox;

import com.codear.engine.service.TestProgressListener;

/** Runs untrusted code against inputs under limits and reports what happened. Docker is one way to do it. */
public interface SandboxRunner {

    /**
     * @param progress told when the run starts and as each test starts and finishes; may be null
     * @throws SandboxException when the sandbox itself fails
     */
    SandboxResult run(SandboxRequest request, TestProgressListener progress);
}
