package com.codear.engine.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;

class ResultArchiveReaderTest {

    private final ResultArchiveReader reader = new ResultArchiveReader();

    /** Builds a tar the way `docker cp container:/dir/results/. -` does: entries named "./name". */
    private static ByteArrayInputStream tar(Map<String, String> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bytes)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                byte[] data = file.getValue().getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry entry = new TarArchiveEntry("./" + file.getKey());
                entry.setSize(data.length);
                tar.putArchiveEntry(entry);
                tar.write(data);
                tar.closeArchiveEntry();
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static Map<String, String> files(String... namesAndContents) {
        Map<String, String> files = new LinkedHashMap<>();
        for (int i = 0; i < namesAndContents.length; i += 2) {
            files.put(namesAndContents[i], namesAndContents[i + 1]);
        }
        return files;
    }

    @Test
    void aCompleteRunBecomesOneTestRunPerTest() throws IOException {
        SandboxResult result = reader.read(tar(files(
                "compile.status", "0", "compile.txt", "",
                "code_0", "0", "meta_0", "0.04 0.03 0.01 9000\n", "out_0", "3\n", "err_0", "",
                "code_1", "0", "meta_1", "0.05 0.04 0.00 9100\n", "out_1", "30\n", "err_1", "",
                "done", "")), 2, 1_000_000, false);

        assertTrue(result.finished());
        assertTrue(result.compile().ok());
        assertEquals(2, result.runs().size());
        TestRun first = result.runFor(0).orElseThrow();
        assertEquals(0, first.exitCode());
        assertEquals(40, first.wallMs());
        assertEquals(40, first.cpuMs());
        assertEquals(9000, first.peakKb());
        assertEquals("3\n", first.stdout());
        assertEquals(2, first.stdoutBytes());
        assertEquals("30\n", result.runFor(1).orElseThrow().stdout());
    }

    @Test
    void aCrashKeepsItsExitCodeAndErrorOutput() throws IOException {
        SandboxResult result = reader.read(tar(files(
                "compile.status", "0",
                "code_0", "139", "meta_0", "Command terminated by signal 11\n0.19 0.10 0.00 8932\n",
                "out_0", "", "err_0", "timeout: the monitored command dumped core",
                "done", "")), 3, 1_000_000, false);

        TestRun run = result.runFor(0).orElseThrow();
        assertEquals(139, run.exitCode());
        assertEquals(100, run.cpuMs());
        assertEquals("timeout: the monitored command dumped core", run.stderrTail());
        assertTrue(result.runFor(1).isEmpty(), "the runner stopped after the crash");
    }

    @Test
    void aCompileFailureCarriesTheCompilerOutput() throws IOException {
        SandboxResult result = reader.read(tar(files(
                "compile.status", "1", "compile.txt", "code.cpp:1:1: error: stray", "done", "")), 3, 1_000_000, false);

        assertFalse(result.compile().ok());
        assertEquals(1, result.compile().exitCode());
        assertEquals("code.cpp:1:1: error: stray", result.compile().output());
        assertTrue(result.runs().isEmpty());
    }

    @Test
    void aContainerCutOffBeforeCompilingCountsAsACompileTimeout() throws IOException {
        SandboxResult result = reader.read(tar(files("compile.txt", "")), 2, 1_000_000, true);

        assertFalse(result.finished());
        assertTrue(result.compile().timedOut());
        assertTrue(result.oomKilled());
    }

    @Test
    void aFileOverTheCapIsCutOneByteOverSoAFloodCanBeTold() throws IOException {
        SandboxResult result = reader.read(tar(files(
                "compile.status", "0", "code_0", "153", "meta_0", "0.1 0.1 0 1", "out_0", "x".repeat(10_000),
                "err_0", "", "done", "")), 1, 1_000, false);

        assertEquals(1_001, result.runFor(0).orElseThrow().stdoutBytes());
    }

    @Test
    void onlyTheEndOfALongErrorOutputIsKept() throws IOException {
        String stderr = "A".repeat(10_000) + "THE-END";

        SandboxResult result = reader.read(tar(files(
                "compile.status", "0", "code_0", "1", "meta_0", "0.1 0.1 0 1", "out_0", "", "err_0", stderr,
                "done", "")), 1, 1_000_000, false);

        String tail = result.runFor(0).orElseThrow().stderrTail();
        assertEquals(4096, tail.length());
        assertTrue(tail.endsWith("THE-END"));
    }

    @Test
    void entriesNamedWithADirectoryPrefixAreStillFound() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bytes)) {
            for (Map.Entry<String, String> file : files("results/compile.status", "0", "results/done", "").entrySet()) {
                byte[] data = file.getValue().getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry entry = new TarArchiveEntry(file.getKey());
                entry.setSize(data.length);
                tar.putArchiveEntry(entry);
                tar.write(data);
                tar.closeArchiveEntry();
            }
        }

        SandboxResult result = reader.read(new ByteArrayInputStream(bytes.toByteArray()), 1, 1_000, false);

        assertTrue(result.finished());
        assertTrue(result.compile().ok());
    }

    @Test
    void invalidUtf8InAProgramsOutputIsReplacedNotFatal() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bytes)) {
            byte[] bad = {'o', 'k', (byte) 0xFF, (byte) 0xFE};
            TarArchiveEntry entry = new TarArchiveEntry("out_0");
            entry.setSize(bad.length);
            tar.putArchiveEntry(entry);
            tar.write(bad);
            tar.closeArchiveEntry();
            for (String name : new String[] {"compile.status", "code_0"}) {
                byte[] zero = "0".getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry e = new TarArchiveEntry(name);
                e.setSize(zero.length);
                tar.putArchiveEntry(e);
                tar.write(zero);
                tar.closeArchiveEntry();
            }
        }

        SandboxResult result = reader.read(new ByteArrayInputStream(bytes.toByteArray()), 1, 1_000, false);

        assertTrue(result.runFor(0).orElseThrow().stdout().startsWith("ok"));
    }
}
