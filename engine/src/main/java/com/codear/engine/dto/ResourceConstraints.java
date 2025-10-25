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
}
