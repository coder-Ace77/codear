package com.codear.engine.sandbox;

/**
 * What happened to one test inside the sandbox, as the runner recorded it.
 *
 * @param index       0-based test index
 * @param exitCode    as the runner saw it: 0 fine, 124 wall time, 137 killed, 128+n a signal
 * @param cpuMs       user plus system CPU time
 * @param peakKb      peak resident memory
 * @param stdout      what the program printed (capped by the output limit)
 * @param stdoutBytes size of that output in bytes
 * @param stderrTail  the end of the program's error output
 */
public record TestRun(int index, int exitCode, long wallMs, long cpuMs, long peakKb,
        String stdout, long stdoutBytes, String stderrTail) {
}
