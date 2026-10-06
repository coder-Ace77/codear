package com.codear.engine.judge;

/** Where a failure happened, for the words of a message: "on test 3 of 26", or nothing for a person's own run. */
public record TestLocation(int number, int total) {

    private static final TestLocation CUSTOM_RUN = new TestLocation(0, 0);

    public static TestLocation customRun() {
        return CUSTOM_RUN;
    }

    public boolean isCustomRun() {
        return number == 0;
    }

    /** " on test 3 of 26" */
    public String phrase() {
        return isCustomRun() ? "" : " on test " + number + " of " + total;
    }
}
