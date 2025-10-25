package com.codear.engine.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays; 
import java.util.Collections; 
import java.util.List; 
import java.util.stream.Collectors; 

import org.springframework.stereotype.Service;

import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.entity.LanguageConfig;

import lombok.AllArgsConstructor;


@Service
@AllArgsConstructor
public class EngineService {

    private final ContainerFactory containerFactory;

    public List<String> runCode(String code, String lang, List<String> inputs,ResourceConstraints resourceConstraints){

        System.out.println("[RUN_CODE] Received new request. Language: " + lang);

        if (inputs == null || inputs.isEmpty()) {
            System.out.println("[RUN_CODE] No inputs provided. Skipping execution.");
            return Collections.emptyList();
        }

        Path tempDir = null;
        String containerId = null;

        try {
            LanguageConfig config = LanguageConfigFactory.getLanguageConfig(lang);

            tempDir = containerFactory.createTempCodeFiles(code, inputs, config.getFileName());

            // containerFactory.pullImage(config.getImage());

            containerId = containerFactory.createContainer(config, tempDir, resourceConstraints.getMemoryLimitMb(),inputs.size());

            
            String logs = containerFactory.runContainerAndGetLogs(containerId,resourceConstraints.getTimeLimitMs());
            System.out.println("[RUN_CODE]   Raw Combined Output:\n" + logs);
            
            String[] outputs = logs.split(ContainerFactory.OUTPUT_SEPARATOR + "\n?");
            
            return Arrays.stream(outputs).collect(Collectors.toList());

        } catch (UnsupportedOperationException e) {
            System.err.println("[RUN_CODE] Error: " + e.getMessage());
            return List.of(e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("[RUN_CODE] Execution failed (IO/Interrupted/Timeout): " + e.getMessage());
            e.printStackTrace();
            return List.of("Execution failed (e.g., timeout or I/O error): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[RUN_CODE] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return List.of("Unexpected error: " + e.getMessage());
        } finally {
            System.out.println("[RUN_CODE] Starting cleanup...");
            containerFactory.cleanupContainer(containerId);
            containerFactory.cleanupTempDirectory(tempDir);
            System.out.println("--- [ END ] ---");
        }
    }
}