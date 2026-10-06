package com.codear.engine.service;

/** Told what the sandbox is doing while a submission runs. Test numbers are 1-based. */
public interface TestProgressListener {

    /** The container has started (a compiled language is compiling until the first test starts). */
    default void containerStarted() {
    }

    default void testStarted(int number) {
    }

    default void testFinished(int number) {
    }

    /** All tests ran; the outputs are being compared. */
    default void judging() {
    }
}
