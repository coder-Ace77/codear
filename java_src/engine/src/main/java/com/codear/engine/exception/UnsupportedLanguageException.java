package com.codear.engine.exception;

public class UnsupportedLanguageException extends RuntimeException {

    public UnsupportedLanguageException(String name) {
        super("Unsupported language: " + name);
    }
}
