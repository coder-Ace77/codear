package com.codear.engine.util.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MetricsTest {

    @Test
    void readsWallCpuAndPeakMemory() {
        ResultArchiveReader.Metrics metrics = ResultArchiveReader.Metrics.parse("0.75 0.70 0.02 8888\n");

        assertEquals(750, metrics.getWallMs());
        assertEquals(720, metrics.getCpuMs());
        assertEquals(8888, metrics.getPeakKb());
    }

    @Test
    void theNumbersAreOnTheLastLineAfterATimeNotice() {
        assertEquals(30, ResultArchiveReader.Metrics.parse("Command exited with non-zero status 1\n0.03 0.02 0.01 9112\n").getCpuMs());
        assertEquals(190, ResultArchiveReader.Metrics.parse("Command terminated by signal 11\n0.19 0.10 0.09 8932").getWallMs());
    }

    @Test
    void roundsToTheNearestMillisecond() {
        assertEquals(1, ResultArchiveReader.Metrics.parse("0.0004 0.0006 0.0000 1").getCpuMs());
        assertEquals(0, ResultArchiveReader.Metrics.parse("0.0001 0.0004 0.0000 1").getCpuMs());
    }

    @Test
    void anythingUnreadableIsUnknownNotAnError() {
        assertEquals(ResultArchiveReader.Metrics.UNKNOWN, ResultArchiveReader.Metrics.parse(null));
        assertEquals(ResultArchiveReader.Metrics.UNKNOWN, ResultArchiveReader.Metrics.parse(""));
        assertEquals(ResultArchiveReader.Metrics.UNKNOWN, ResultArchiveReader.Metrics.parse("  \n \n"));
        assertEquals(ResultArchiveReader.Metrics.UNKNOWN, ResultArchiveReader.Metrics.parse("garbage"));
        assertEquals(ResultArchiveReader.Metrics.UNKNOWN, ResultArchiveReader.Metrics.parse("1 2 3"));
        assertEquals(ResultArchiveReader.Metrics.UNKNOWN, ResultArchiveReader.Metrics.parse("a b c d"));
    }
}
