package com.codear.problem.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemSendDTO {
    private Long id;
    private String title;
    private String description;
    private String inputDescription;
    private String outputDescription;
    private String constraints;
    private String difficulty;

    private List<String> tags;

    private Long timeLimitMs;

    private Integer memoryLimitMb;
}
