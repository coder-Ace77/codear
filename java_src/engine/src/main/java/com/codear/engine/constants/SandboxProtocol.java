package com.codear.engine.constants;

/**
 * The contract between the engine and resources/sandbox/runner.sh: file names, exit codes and markers.
 * Change one side and the other must change with it.
 */
public final class SandboxProtocol {

    private SandboxProtocol() {
    }

    public static final String RUNNER_RESOURCE = "sandbox/runner.sh";
    public static final String RUNNER_FILE = "runner.sh";
    public static final String RESULTS_DIR = "results";

    public static final String COMPILE_STATUS_FILE = "compile.status";
    public static final String COMPILE_OUTPUT_FILE = "compile.txt";
    public static final String DONE_FILE = "done";

    /** Printed after each test so the live progress bar can follow the run. Never used to decide a verdict. */
    public static final String TEST_SEPARATOR = "===CODEAR_TEST_CASE_SEPARATOR===";
    public static final String TEST_START_PREFIX = "[TEST-START-";

    public static String inputFile(int test) {
        return "input_" + test + ".txt";
    }

    public static String outputFile(int test) {
        return "out_" + test;
    }

    public static String errorFile(int test) {
        return "err_" + test;
    }

    public static String exitCodeFile(int test) {
        return "code_" + test;
    }

    public static String metricsFile(int test) {
        return "meta_" + test;
    }

    // Exit codes as the runner records them (128 + the signal number for a killed process).
    public static final int EXIT_OK = 0;
    public static final int EXIT_WALL_TIMEOUT = 124;
    public static final int EXIT_KILLED = 137;
    public static final int EXIT_CPU_LIMIT = 152;
    public static final int EXIT_OUTPUT_LIMIT = 153;

    // Environment variables the runner reads.
    public static final String ENV_LANGUAGE = "LANGUAGE";
    public static final String ENV_NUM_TESTS = "NUM_TESTS";
    public static final String ENV_TIME_LIMIT_MS = "TIME_LIMIT_MS";
    public static final String ENV_CPU_LIMIT_S = "CPU_LIMIT_S";
    public static final String ENV_WALL_LIMIT_S = "WALL_LIMIT_S";
    public static final String ENV_MEMORY_LIMIT_BYTES = "MEMORY_LIMIT_BYTES";
    public static final String ENV_OUTPUT_LIMIT_BYTES = "OUTPUT_LIMIT_BYTES";
    public static final String ENV_COMPILE_TIMEOUT_S = "COMPILE_TIMEOUT_S";
    public static final String ENV_RUN_AS = "RUN_AS";
    public static final String ENV_TEST_SEPARATOR = "TEST_SEPARATOR";
}
