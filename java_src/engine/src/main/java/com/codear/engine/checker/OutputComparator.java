package com.codear.engine.checker;

/** Decides whether what a program printed counts as the expected answer. */
public interface OutputComparator {

    boolean matches(String expected, String actual);
}
