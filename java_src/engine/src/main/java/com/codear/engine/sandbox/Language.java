package com.codear.engine.sandbox;

import java.util.Arrays;

/** A language the judge can run: how it is named in a submission, the source file, and the sandbox image. */
public enum Language {
    PYTHON("python", "code.py", "codear-python:latest"),
    CPP("cpp", "code.cpp", "codear-cpp:latest");

    private final String id;
    private final String sourceFile;
    private final String defaultImage;

    Language(String id, String sourceFile, String defaultImage) {
        this.id = id;
        this.sourceFile = sourceFile;
        this.defaultImage = defaultImage;
    }

    public String id() {
        return id;
    }

    public String sourceFile() {
        return sourceFile;
    }

    public String defaultImage() {
        return defaultImage;
    }

    /** @throws UnsupportedLanguageException for a name that is not supported */
    public static Language from(String name) {
        return Arrays.stream(values())
                .filter(language -> language.id.equalsIgnoreCase(name == null ? "" : name.trim()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedLanguageException(name));
    }

    public static class UnsupportedLanguageException extends RuntimeException {
        public UnsupportedLanguageException(String name) {
            super("Unsupported language: " + name);
        }
    }
}
