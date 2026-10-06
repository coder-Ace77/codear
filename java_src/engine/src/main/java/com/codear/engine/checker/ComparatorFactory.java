package com.codear.engine.checker;

/** Builds the comparator a problem asks for. */
public final class ComparatorFactory {

    public static final double DEFAULT_FLOAT_TOLERANCE = 1e-6;

    private ComparatorFactory() {
    }

    public static OutputComparator create(CheckerMode mode, Double tolerance) {
        return switch (mode) {
            case TOKENS -> new TokenComparator(false);
            case TOKENS_IGNORE_CASE -> new TokenComparator(true);
            case EXACT_LINES -> new ExactLinesComparator();
            case FLOAT -> new FloatTokenComparator(tolerance != null && tolerance > 0 ? tolerance : DEFAULT_FLOAT_TOLERANCE);
        };
    }

    /** The comparator a problem's settings ask for; anything unset or unrecognised means the default. */
    public static OutputComparator forProblem(String modeName, Double tolerance) {
        return create(CheckerMode.fromName(modeName), tolerance);
    }

    public static OutputComparator defaultComparator() {
        return create(CheckerMode.TOKENS, null);
    }
}
