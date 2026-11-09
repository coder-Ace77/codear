package com.codear.engine.checker;

import com.codear.engine.dto.ResourceConstraints;

public final class ComparatorFactory {

    private static final double DEFAULT_TOLERANCE = 1e-6;

    private ComparatorFactory() {
    }

    public static OutputComparator getCheckerFromConstraints(ResourceConstraints constraints) {
        if (constraints != null && "FLOAT".equalsIgnoreCase(constraints.getChecker())) {
            Double tolerance = constraints.getCheckerTolerance();
            return new FloatComparator(tolerance != null && tolerance > 0 ? tolerance : DEFAULT_TOLERANCE);
        }
        return new TokenComparator();
    }
}
