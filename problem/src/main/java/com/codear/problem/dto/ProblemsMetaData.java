package com.codear.problem.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProblemsMetaData {
    private Long count;
    private List<String> tags;
}
