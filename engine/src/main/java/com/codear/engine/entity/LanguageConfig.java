package com.codear.engine.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class LanguageConfig {

    private String image;
    private String fileName;
    private List<String> cmd;

}
