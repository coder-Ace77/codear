package com.codear.engine.checker;

/** How a problem's answers are compared. TOKENS is the default. */
public enum CheckerMode {
    /** Word by word, ignoring spacing and line breaks; case matters. */
    TOKENS,
    /** Like TOKENS but "Yes" matches "YES". */
    TOKENS_IGNORE_CASE,
    /** Line by line (the original rule): line structure matters, trailing blank space does not. */
    EXACT_LINES,
    /** Like TOKENS, but numbers may differ by a tolerance. */
    FLOAT;

    public static CheckerMode fromName(String name) {
        if (name == null || name.isBlank()) {
            return TOKENS;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TOKENS;
        }
    }
}
