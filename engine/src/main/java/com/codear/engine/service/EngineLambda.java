package com.codear.engine.service;

import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.entity.LanguageConfig;
import com.codear.engine.service.LanguageConfigFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class EngineLambda{

    public List<String> runCode(String code, String lang, List<String> inputs, ResourceConstraints constraints) {
        if (inputs == null || inputs.isEmpty()) {
            return Collections.emptyList();
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory(Path.of("/tmp"), "codear_");
            LanguageConfig config = LanguageConfigFactory.getLanguageConfig(lang);
            
            Path sourceFile = tempDir.resolve(config.getFileName());
            Files.writeString(sourceFile, code);

            if (config.getCompileCmd() != null && !config.getCompileCmd().isEmpty()) {
                String compileError = runCommand(config.getCompileCmd(), tempDir, null, 10000); // 10s compile limit
                if (!compileError.isEmpty()) {
                    return List.of("Compilation Error:\n" + compileError);
                }
            }

            List<String> outputs = new ArrayList<>();
            for (String input : inputs) {
                String result = runCommand(config.getRunCmd(), tempDir, input, constraints.getTimeLimitMs());
                outputs.add(result);
            }
            return outputs;

        } catch (Exception e) {
            return List.of("System Error: " + e.getMessage());
        } finally {
            cleanup(tempDir);
        }
    }

    private String runCommand(List<String> cmd, Path dir, String input, long timeoutMs) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true); 
        
        Process process = pb.start();

        if (input != null) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(input);
                writer.flush();
            }
        }

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            return "Time Limit Exceeded";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void cleanup(Path tempDir) {
        if (tempDir == null) return;
        try (var stream = Files.walk(tempDir)) {
            stream.sorted((a, b) -> b.compareTo(a)) 
                  .map(Path::toFile)
                  .forEach(File::delete);
        } catch (IOException ignored) {}
    }
}