package com.codear.engine.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RunMetricsTest {

    @Test
    void readsWallCpuAndPeakMemory() {
        RunMetrics metrics = RunMetrics.parse("0.75 0.70 0.02 8888\n");

        assertEquals(750, metrics.wallMs());
        assertEquals(720, metrics.cpuMs());
        assertEquals(8888, metrics.peakKb());
    }

    @Test
    void theNumbersAreOnTheLastLineAfterATimeNotice() {
        assertEquals(30, RunMetrics.parse("Command exited with non-zero status 1\n0.03 0.02 0.01 9112\n").cpuMs());
        assertEquals(190, RunMetrics.parse("Command terminated by signal 11\n0.19 0.10 0.09 8932").wallMs());
    }

    @Test
    void roundsToTheNearestMillisecond() {
        assertEquals(1, RunMetrics.parse("0.0004 0.0006 0.0000 1").cpuMs());
        assertEquals(0, RunMetrics.parse("0.0001 0.0004 0.0000 1").cpuMs());
    }

    @Test
    void anythingUnreadableIsUnknownNotAnError() {
        assertEquals(RunMetrics.UNKNOWN, RunMetrics.parse(null));
        assertEquals(RunMetrics.UNKNOWN, RunMetrics.parse(""));
        assertEquals(RunMetrics.UNKNOWN, RunMetrics.parse("  \n \n"));
        assertEquals(RunMetrics.UNKNOWN, RunMetrics.parse("garbage"));
        assertEquals(RunMetrics.UNKNOWN, RunMetrics.parse("1 2 3"));
        assertEquals(RunMetrics.UNKNOWN, RunMetrics.parse("a b c d"));
    }
}
