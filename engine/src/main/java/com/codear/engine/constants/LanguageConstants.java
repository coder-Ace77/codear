package com.codear.engine.constants;

public class LanguageConstants {

    public static final String PYTHON = "python";
    public static final String CPP = "cpp";

    public static final String PYTHON_DOCKER_IMAGE = "python:3.10";
    public static final String CPP_DOCKER_IMAGE = "gcc:latest";

    public static final String PYTHON_FILE_NAME = "code.py";
    public static final String CPP_FILE_NAME = "code.cpp";

    public static final String[] PYTHON_EXECUTION_COMMAND = {"python3", "/app/code.py"};
    public static final String[] CPP_EXECUTION_COMMAND = {"bash", "-c", "g++ /app/code.cpp -o /app/a.out && /app/a.out"};
}
