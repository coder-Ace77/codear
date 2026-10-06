package com.codear.engine.judge;

/** The result of running a person's own input: what to show in the output box, and whether it ran cleanly. */
public record CustomRunResult(String text, boolean success) {
}
