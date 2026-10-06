package com.codear.engine.constants;

import java.util.List;

/** Every tunable number of the judge, in one place. */
public final class JudgeDefaults {

    private JudgeDefaults() {
    }

    // --- limits when a problem does not set its own ---
    public static final long DEFAULT_TIME_LIMIT_MS = 1_000;
    public static final int DEFAULT_MEMORY_LIMIT_MB = 256;

    // --- time ---
    public static final int COMPILE_TIMEOUT_SECONDS = 20;
    /** Wall-clock allowance is a multiple of the CPU limit: it only catches programs that sleep or block. */
    public static final int WALL_LIMIT_FACTOR = 2;
    public static final int WALL_LIMIT_EXTRA_SECONDS = 1;
    public static final int KILL_GRACE_SECONDS = 2;
    /** Container start, copying files in and out. */
    public static final int SANDBOX_STARTUP_GRACE_SECONDS = 30;
    /** A kill that arrives within this fraction of a limit counts as hitting that limit. */
    public static final double LIMIT_HIT_FRACTION = 0.95;

    // --- memory ---
    /** The compiler and the runner live in the same container as the program, so the container gets this much extra. */
    public static final int CONTAINER_MEMORY_HEADROOM_MB = 256;

    // --- output ---
    public static final long MIN_OUTPUT_LIMIT_BYTES = 64 * 1024L;
    public static final long MAX_OUTPUT_LIMIT_BYTES = 8 * 1024 * 1024L;
    /** A program may write this many times the largest expected output before it is called a flood. */
    public static final long OUTPUT_LIMIT_FACTOR = 4;
    public static final long OUTPUT_LIMIT_SLACK_BYTES = 4 * 1024L;
    public static final int MAX_STDERR_BYTES_KEPT = 4 * 1024;

    // --- source code ---
    public static final int MAX_SOURCE_CHARS = 65_536;

    // --- sandbox container ---
    public static final long SANDBOX_PIDS_LIMIT = 64;
    public static final long SANDBOX_NANO_CPUS = 1_000_000_000L;
    /** nobody:nogroup in the sandbox images. */
    public static final String SANDBOX_RUN_AS = "65534:65534";
    public static final String SANDBOX_TMPFS_PATH = "/tmp";
    public static final String SANDBOX_TMPFS_OPTIONS = "rw,exec,nosuid,size=64m";
    public static final String SANDBOX_REMOTE_ROOT = "/app";
    public static final String SANDBOX_LABEL = "codear.sandbox";
    public static final String SANDBOX_CREATED_LABEL = "codear.created";

    // --- messages shown to people ---
    public static final int MAX_COMPILE_MESSAGE_CHARS = 3_000;
    public static final int MAX_DIFF_FIELD_CHARS = 200;
    public static final int MAX_STDERR_MESSAGE_CHARS = 600;
    /** Where the runner compiles; the engine hides this prefix from compiler messages. */
    public static final String COMPILE_DIR_PREFIX = "/tmp/build/";

    /** Text in a crashed program's error output that means it ran out of memory. */
    public static final List<String> MEMORY_ERROR_MARKERS = List.of(
            "bad_alloc", "MemoryError", "Cannot allocate memory", "out of memory");

    // --- the queue ---
    public static final String LISTENER_MAX_CONCURRENT_MESSAGES = "2";
    /** The library refuses to start if a poll may fetch more messages than can run at once (its default is 10). */
    public static final String LISTENER_MAX_MESSAGES_PER_POLL = "2";
    /** Longer than the longest judging run, so a message is never handed to a second worker mid-run. */
    public static final String LISTENER_VISIBILITY_SECONDS = "300";

    // --- cleaning up after crashes ---
    public static final int STALE_CONTAINER_MINUTES = 10;
    public static final int STALE_WORKSPACE_MINUTES = 30;
    public static final long JANITOR_INTERVAL_MS = 5 * 60 * 1000L;
}
