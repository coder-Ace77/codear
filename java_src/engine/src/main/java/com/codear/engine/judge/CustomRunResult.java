package com.codear.engine.judge;

import lombok.Value;

/** The result of running a person's own input: what to show in the output box, and whether it ran cleanly. */
@Value
public class CustomRunResult {

    String text;
    boolean success;
}
