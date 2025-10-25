package com.codear.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemSummaryDTO {
    
    private Long id;
    private String title; 
    private String tags;

}