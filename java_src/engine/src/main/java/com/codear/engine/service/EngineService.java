package com.codear.engine.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.dto.CodeExecutionResult;
import com.codear.engine.entity.LanguageConfig;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class EngineService {

    private final ContainerFactory containerFactory;

    public CodeExecutionResult runCode(String code, String lang, List<String> inputs,
            ResourceConstraints resourceConstraints){
        if (inputs == null || inputs.isEmpty()){
            return CodeExecutionResult.builder()
                    .logs("No inputs provided")
                    .outputs(Collections.emptyList())
                    .cpuTimeMs(0L)
                    .memoryUsedPk("0MB")
                    .build();
        }

        Path tempDir = null;
        String containerId = null;

        try {
            LanguageConfig config = LanguageConfigFactory.getLanguageConfig(lang);
            tempDir = containerFactory.createTempCodeFiles(code, inputs, config.getFileName());
            containerId = containerFactory.createContainer(config, tempDir, resourceConstraints.getMemoryLimitMb(),inputs.size());

            CodeExecutionResult executionResult = containerFactory.runContainerAndGetLogs(containerId,inputs.size(),
                    resourceConstraints.getTimeLimitMs() != null ? resourceConstraints.getTimeLimitMs() : 1000L);

            String[] outputArray = executionResult.getLogs().split(ContainerFactory.OUTPUT_SEPARATOR + "\\n?");
            List<String> cleanedOutputs = Arrays.stream(outputArray)
                    .map(s -> s.replaceAll("METRICS:[\\d\\.]+:\\d+\\n?", "").trim()) // Remove metrics and trim
                    .collect(Collectors.toList());
            executionResult.setOutputs(cleanedOutputs);
            return executionResult;
        }catch(UnsupportedOperationException e){
            return CodeExecutionResult.builder().logs(e.getMessage()).outputs(List.of(e.getMessage())).build();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return CodeExecutionResult.builder().logs(e.getMessage())
                    .outputs(List.of("Execution failed: " + e.getMessage())).build();
        } catch (Exception e) {
            e.printStackTrace();
            return CodeExecutionResult.builder().logs(e.getMessage())
                    .outputs(List.of("Unexpected error: " + e.getMessage())).build();
        } finally {
            containerFactory.cleanupContainer(containerId);
            containerFactory.cleanupTempDirectory(tempDir);
        }
    }
}