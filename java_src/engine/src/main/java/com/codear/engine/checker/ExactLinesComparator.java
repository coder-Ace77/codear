package com.codear.engine.checker;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The original rule: each line is trimmed, blank lines are dropped, and what is left must be identical, so line
 * structure matters but trailing spaces and blank lines do not.
 */
public final class ExactLinesComparator implements OutputComparator {

    @Override
    public boolean matches(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
    }
}
