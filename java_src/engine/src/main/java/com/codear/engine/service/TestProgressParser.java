package com.codear.engine.service;

import com.codear.engine.constants.SandboxProtocol;

/**
 * Reads the container's output as it streams and reports which test is running.
 *
 * The run script prints "[TEST-START-i]" before test i (0-based) and OUTPUT_SEPARATOR after it, each on its
 * own line. Output arrives in arbitrary chunks that can split a line, so only complete lines are looked at.
 *
 * This only drives the progress bar. The verdict never depends on it.
 */
public class TestProgressParser {

    private static final String START_PREFIX = SandboxProtocol.TEST_START_PREFIX;
    /** A marker line is short; anything longer without a newline is program output and is dropped. */
    private static final int MAX_PENDING_CHARS = 256;

    private final TestProgressListener listener;
    private final StringBuilder pending = new StringBuilder();
    private int finished = 0;

    public TestProgressParser(TestProgressListener listener) {
        this.listener = listener;
    }

    public void feed(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        pending.append(chunk);

        int newline;
        while ((newline = pending.indexOf("\n")) >= 0) {
            String line = pending.substring(0, newline).trim();
            pending.delete(0, newline + 1);
            handle(line);
        }

        if (pending.length() > MAX_PENDING_CHARS) {
            pending.delete(0, pending.length() - MAX_PENDING_CHARS);
        }
    }

    private void handle(String line) {
        if (line.equals(SandboxProtocol.TEST_SEPARATOR)) {
            finished++;
            listener.testFinished(finished);
        } else if (line.startsWith(START_PREFIX) && line.endsWith("]")) {
            try {
                int index = Integer.parseInt(line.substring(START_PREFIX.length(), line.length() - 1));
                listener.testStarted(index + 1);
            } catch (NumberFormatException ignored) {
                // not one of ours: a program that prints something that merely looks like a marker
            }
        }
    }
}
