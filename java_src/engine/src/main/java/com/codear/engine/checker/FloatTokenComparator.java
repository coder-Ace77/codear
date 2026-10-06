package com.codear.engine.checker;

/**
 * Word by word like {@link TokenComparator}, but two numbers match when they differ by at most `tolerance`,
 * absolute or relative to the expected value, whichever is looser. Words that are not both numbers must be equal.
 */
public final class FloatTokenComparator implements OutputComparator {

    private final double tolerance;

    public FloatTokenComparator(double tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public boolean matches(String expected, String actual) {
        Tokens wanted = new Tokens(expected);
        Tokens got = new Tokens(actual);
        while (true) {
            String a = wanted.next();
            String b = got.next();
            if (a == null || b == null) {
                return a == null && b == null;
            }
            if (!tokensMatch(a, b)) {
                return false;
            }
        }
    }

    private boolean tokensMatch(String expected, String actual) {
        if (expected.equals(actual)) {
            return true;
        }
        try {
            double want = Double.parseDouble(expected);
            double have = Double.parseDouble(actual);
            if (Double.isNaN(want) || Double.isNaN(have) || Double.isInfinite(want) || Double.isInfinite(have)) {
                return false;
            }
            double difference = Math.abs(want - have);
            return difference <= tolerance || difference <= tolerance * Math.abs(want);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
