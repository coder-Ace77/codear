package com.codear.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemSummaryDTO {
    private Long id;
    private String title; 
    private List<String> tags;
    private String difficulty;
}