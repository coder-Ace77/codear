package com.codear.problem.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestDTO {
    private Long userId;
    private String code;
    private String language;
    private Long problemId;
    private String input;   
    private String output;
    private String status;
    private String submissionId; 
}
