package com.codear.engine.checker;

import com.codear.engine.dto.ResourceConstraints;

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

    /** The comparator for a problem's stored settings; a problem without settings gets the default. */
    public static OutputComparator forConstraints(ResourceConstraints constraints) {
        return constraints == null
                ? defaultComparator()
                : forProblem(constraints.getChecker(), constraints.getCheckerTolerance());
    }

    public static OutputComparator defaultComparator() {
        return create(CheckerMode.TOKENS, null);
    }
}
