package com.codear.engine.sandbox;

/** The sandbox itself failed (Docker, files, results), as opposed to the submitted program failing. */
public class SandboxException extends RuntimeException {

    public SandboxException(String message) {
        super(message);
    }

    public SandboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
