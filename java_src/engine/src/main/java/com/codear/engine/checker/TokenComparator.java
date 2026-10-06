package com.codear.engine.checker;

/**
 * Compares word by word and ignores all spacing and line breaks: "1 2\n3" matches "1\n2 3". Case matters unless
 * the comparator was built to ignore it.
 */
public final class TokenComparator implements OutputComparator {

    private final boolean ignoreCase;

    public TokenComparator(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
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
            boolean same = ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b);
            if (!same) {
                return false;
            }
        }
    }
}
