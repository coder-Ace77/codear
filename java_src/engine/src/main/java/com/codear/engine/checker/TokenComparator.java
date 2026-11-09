package com.codear.engine.checker;

public class TokenComparator implements OutputComparator {

    @Override
    public boolean matches(String expected, String actual) {
        String[] want = split(expected);
        String[] got = split(actual);
        if (want.length != got.length) {
            return false;
        }
        for (int i = 0; i < want.length; i++) {
            if (!want[i].equals(got[i])) {
                return false;
            }
        }
        return true;
    }

    static String[] split(String text) {
        String trimmed = text == null ? "" : text.strip();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }
}
