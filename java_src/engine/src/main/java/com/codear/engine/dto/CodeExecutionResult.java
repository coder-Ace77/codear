package com.codear.engine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CodeExecutionResult {
    private String logs;
    private java.util.List<String> outputs;
    private Long cpuTimeMs;
    private String memoryUsedPk;
}
