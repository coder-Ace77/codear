package com.codear.problem.dto;

import lombok.Data;
import java.time.LocalDateTime; 

@Data
public class Code {
    private Long userId;
    private String code;
    private String language;
    private Long problemId;
    
    private LocalDateTime submittedAt; 
}