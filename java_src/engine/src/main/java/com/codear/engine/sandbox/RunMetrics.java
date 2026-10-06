package com.codear.engine.sandbox;

/** The numbers GNU time wrote for one test: "<wall s> <user s> <sys s> <peak KB>". */
public record RunMetrics(long wallMs, long cpuMs, long peakKb) {

    public static final RunMetrics UNKNOWN = new RunMetrics(0, 0, 0);

    /**
     * GNU time puts a line such as "Command exited with non-zero status 1" or "Command terminated by signal 11"
     * before the numbers, so the numbers are always on the last non-blank line.
     */
    public static RunMetrics parse(String text) {
        if (text == null) {
            return UNKNOWN;
        }
        String last = null;
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                last = line.trim();
            }
        }
        if (last == null) {
            return UNKNOWN;
        }
        String[] parts = last.split("\\s+");
        if (parts.length != 4) {
            return UNKNOWN;
        }
        try {
            double wall = Double.parseDouble(parts[0]);
            double user = Double.parseDouble(parts[1]);
            double system = Double.parseDouble(parts[2]);
            return new RunMetrics(toMs(wall), toMs(user + system), Long.parseLong(parts[3]));
        } catch (NumberFormatException e) {
            return UNKNOWN;
        }
    }

    private static long toMs(double seconds) {
        return Math.round(seconds * 1000);
    }
}
