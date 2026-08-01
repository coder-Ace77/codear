package com.codear.engine.dto;

import lombok.Data;
import java.time.LocalDateTime; 

@Data
public class Code {
    private String submissionId;
    private Long userId;
    private String code;
    private String language;
    private Long problemId;
    
    private LocalDateTime submittedAt; 
}