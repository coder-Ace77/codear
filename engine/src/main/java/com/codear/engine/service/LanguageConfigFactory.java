package com.codear.engine.service;

import java.util.List;
import com.codear.engine.constants.LanguageConstants;
import com.codear.engine.entity.LanguageConfig;

public class LanguageConfigFactory {
    public static LanguageConfig getLanguageConfig(String lang) {
        if (lang.equalsIgnoreCase("python")){
            return new LanguageConfig(
                    LanguageConstants.PYTHON_DOCKER_IMAGE,
                    LanguageConstants.PYTHON_FILE_NAME,
                    List.of(LanguageConstants.PYTHON_EXECUTION_COMMAND)
            );
        } else if (lang.equalsIgnoreCase("cpp")) {
            return new LanguageConfig(
                    LanguageConstants.CPP_DOCKER_IMAGE,
                    LanguageConstants.CPP_FILE_NAME,
                    List.of(LanguageConstants.CPP_EXECUTION_COMMAND)
            );
        } else {
            throw new UnsupportedOperationException("Unsupported language: " + lang);
        }
    }   
}
