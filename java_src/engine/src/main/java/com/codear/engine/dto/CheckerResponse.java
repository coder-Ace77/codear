package com.codear.engine.dto;

import com.codear.engine.enums.RunStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckerResponse {

    private RunStatus status;
    private String msg;

    private Integer totalTests;
    private Integer PassedTests;
    
}
