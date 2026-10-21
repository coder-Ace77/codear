package com.codear.engine.enums;

import java.util.Arrays;

import com.codear.engine.exception.UnsupportedLanguageException;

/** A language the judge can run: how it is named in a submission and the source file it is saved as. */
public enum Language {
    PYTHON("python", "code.py"),
    CPP("cpp", "code.cpp");

    private final String id;
    private final String sourceFile;

    Language(String id, String sourceFile) {
        this.id = id;
        this.sourceFile = sourceFile;
    }

    public String id() {
        return id;
    }

    public String sourceFile() {
        return sourceFile;
    }

    /** @throws UnsupportedLanguageException for a name that is not supported */
    public static Language from(String name) {
        return Arrays.stream(values())
                .filter(language -> language.id.equalsIgnoreCase(name == null ? "" : name.trim()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedLanguageException(name));
    }
}
