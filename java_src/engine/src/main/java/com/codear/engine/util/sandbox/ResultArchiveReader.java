package com.codear.engine.util.sandbox;

import static com.codear.engine.constants.JudgeDefaults.MAX_STDERR_BYTES_KEPT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.codear.engine.constants.SandboxProtocol;
import com.codear.engine.dto.sandbox.SandboxResult;
import com.codear.engine.dto.sandbox.SandboxResult.CompileResult;
import com.codear.engine.dto.sandbox.SandboxResult.TestRun;
import com.codear.engine.enums.CompileStatus;

import lombok.Value;

/**
 * Turns the tar archive of the runner's results folder into a {@link SandboxResult}. Each file is read up to a
 * size cap, so a program's output can never make the engine hold more than that in memory.
 */
public final class ResultArchiveReader {

    public SandboxResult read(InputStream archive, int numTests, long maxFileBytes, boolean oomKilled)
            throws IOException {
        Map<String, byte[]> files = readFiles(archive, maxFileBytes);
        boolean finished = files.containsKey(SandboxProtocol.DONE_FILE);
        return new SandboxResult(compileResult(files, finished), runs(files, numTests), finished, oomKilled);
    }

    private Map<String, byte[]> readFiles(InputStream archive, long maxFileBytes) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        try (TarArchiveInputStream tar = new TarArchiveInputStream(archive)) {
            ArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                // the cap is one byte over the limit so a file that hit the limit can be told from one that just fits
                files.put(baseName(entry.getName()), tar.readNBytes((int) Math.min(maxFileBytes + 1, Integer.MAX_VALUE)));
            }
        }
        return files;
    }

    private CompileResult compileResult(Map<String, byte[]> files, boolean finished) {
        String output = text(files.get(SandboxProtocol.COMPILE_OUTPUT_FILE));
        Integer status = parseInt(text(files.get(SandboxProtocol.COMPILE_STATUS_FILE)));
        if (status == null) {
            // the container was cut off before the compile step reported: treat it as the compile running out of time
            return new CompileResult(finished ? CompileStatus.FAILED : CompileStatus.TIMED_OUT, output);
        }
        return new CompileResult(CompileStatus.fromExitCode(status), output);
    }

    private List<TestRun> runs(Map<String, byte[]> files, int numTests) {
        List<TestRun> runs = new ArrayList<>();
        for (int i = 0; i < numTests; i++) {
            Integer exit = parseInt(text(files.get(SandboxProtocol.exitCodeFile(i))));
            if (exit == null) {
                continue;
            }
            Metrics metrics = Metrics.parse(text(files.get(SandboxProtocol.metricsFile(i))));
            byte[] out = files.getOrDefault(SandboxProtocol.outputFile(i), new byte[0]);
            runs.add(new TestRun(i, exit, metrics.getWallMs(), metrics.getCpuMs(), metrics.getPeakKb(),
                    new String(out, StandardCharsets.UTF_8), out.length,
                    tail(files.get(SandboxProtocol.errorFile(i)))));
        }
        return runs;
    }

    private static String tail(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        int start = Math.max(0, bytes.length - MAX_STDERR_BYTES_KEPT);
        return new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
    }

    private static String text(byte[] bytes) {
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    private static Integer parseInt(String value) {
        try {
            return value.isBlank() ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Archive entries may be named "meta_0", "./meta_0" or "results/meta_0". */
    private static String baseName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /** The numbers GNU time wrote for one test: "<wall s> <user s> <sys s> <peak KB>". */
    @Value
    static class Metrics {

        long wallMs;
        long cpuMs;
        long peakKb;

        static final Metrics UNKNOWN = new Metrics(0, 0, 0);

        /**
         * GNU time puts a line such as "Command exited with non-zero status 1" or "Command terminated by signal 11"
         * before the numbers, so the numbers are always on the last non-blank line.
         */
        static Metrics parse(String text) {
            if (text == null) {
                return UNKNOWN;
            }
            String last = null;
            for (String line : text.split("\\R")) {
                if (!line.isBlank()) {
                    last = line.trim();
                }
            }
            if (last == null) {
                return UNKNOWN;
            }
            String[] parts = last.split("\\s+");
            if (parts.length != 4) {
                return UNKNOWN;
            }
            try {
                double wall = Double.parseDouble(parts[0]);
                double user = Double.parseDouble(parts[1]);
                double system = Double.parseDouble(parts[2]);
                return new Metrics(toMs(wall), toMs(user + system), Long.parseLong(parts[3]));
            } catch (NumberFormatException e) {
                return UNKNOWN;
            }
        }

        private static long toMs(double seconds) {
            return Math.round(seconds * 1000);
        }
    }
}
