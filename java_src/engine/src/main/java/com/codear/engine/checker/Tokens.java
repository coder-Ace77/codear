package com.codear.engine.checker;

/**
 * Walks a text one whitespace-separated token at a time without building arrays, so comparing a multi-megabyte
 * output does not allocate a copy of it.
 */
final class Tokens {

    private final String text;
    private int position = 0;

    Tokens(String text) {
        this.text = text == null ? "" : text;
    }

    /** The next token, or null when the text is used up. */
    String next() {
        int length = text.length();
        while (position < length && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
        if (position >= length) {
            return null;
        }
        int start = position;
        while (position < length && !Character.isWhitespace(text.charAt(position))) {
            position++;
        }
        return text.substring(start, position);
    }
}
