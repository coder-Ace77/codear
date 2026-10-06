package com.codear.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceConstraints {

    private Long timeLimitMs;
    private Integer memoryLimitMb;
    private String difficulty;
    /** A CheckerMode name, or null for the default. */
    private String checker;
    private Double checkerTolerance;

    public ResourceConstraints(Long timeLimitMs, Integer memoryLimitMb, String difficulty) {
        this(timeLimitMs, memoryLimitMb, difficulty, null, null);
    }
}
