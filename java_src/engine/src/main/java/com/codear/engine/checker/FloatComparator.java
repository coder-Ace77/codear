package com.codear.engine.checker;

public class FloatComparator implements OutputComparator {

    private final double tolerance;

    public FloatComparator(double tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public boolean matches(String expected, String actual) {
        String[] want = TokenComparator.split(expected);
        String[] got = TokenComparator.split(actual);
        if (want.length != got.length) {
            return false;
        }
        for (int i = 0; i < want.length; i++) {
            if (!want[i].equals(got[i]) && !withinDelta(want[i], got[i])){
                return false;
            }
        }
        return true;
    }

    private boolean withinDelta(String expected, String actual){
        try {
            double want = Double.parseDouble(expected);
            double have = Double.parseDouble(actual);
            if (!Double.isFinite(want) || !Double.isFinite(have)) {
                return false;
            }
            double difference = Math.abs(want - have);
            return difference <= tolerance || difference <= tolerance * Math.abs(want);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
