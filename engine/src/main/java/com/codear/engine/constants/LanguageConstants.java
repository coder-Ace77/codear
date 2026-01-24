package com.codear.engine.constants;

public class LanguageConstants {

        public static final String PYTHON = "python";
        public static final String CPP = "cpp";

        public static final String PYTHON_DOCKER_IMAGE = "codear-python:latest";
        public static final String CPP_DOCKER_IMAGE = "codear-cpp:latest";

        public static final String PYTHON_FILE_NAME = "code.py";
        public static final String CPP_FILE_NAME = "code.cpp";

        public static final String[] PYTHON_EXECUTION_COMMAND = { "bash", "-c",
                        "ulimit -s 262144 > /dev/null 2>&1 && python3 code.py" };
        public static final String[] CPP_EXECUTION_COMMAND = { "bash", "-c",
                        "g++ code.cpp -o a.out -O3 && ulimit -s 262144 > /dev/null 2>&1 && ./a.out" };

        public static final String[] CPP_COMPILE_CMD = { "g++", "code.cpp", "-o", "a.out", "-O3" };
        public static final String[] CPP_RUN_CMD = { "bash", "-c", "ulimit -s 262144 > /dev/null 2>&1 && ./a.out" };

        public static final String[] SUPPORTED_LANGUAGES = { PYTHON, CPP };

}
