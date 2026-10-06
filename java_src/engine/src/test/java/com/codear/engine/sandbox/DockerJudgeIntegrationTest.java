package com.codear.engine.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.codear.engine.checker.ComparatorFactory;
import com.codear.engine.dto.ResourceConstraints;
import com.codear.engine.judge.CustomRunResult;
import com.codear.engine.judge.ExpectedTest;
import com.codear.engine.judge.JudgeReport;
import com.codear.engine.judge.JudgeRequest;
import com.codear.engine.judge.JudgeService;
import com.codear.engine.judge.Verdict;
import com.codear.engine.service.TestProgressListener;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

/**
 * The whole judge against the real sandbox: runner script, Docker restrictions, result files and verdict rules,
 * with programs that crash, loop, eat memory, flood output, read other tests' inputs and try to forge results.
 *
 * Needs Docker and two local images (built from engine/docker, any names): codear-python-test and codear-cpp-test.
 * Skipped when they are not there, so the normal build never depends on Docker.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(120)
class DockerJudgeIntegrationTest {

    private static final String PYTHON_IMAGE = "codear-python-test:latest";
    private static final String CPP_IMAGE = "codear-cpp-test:latest";

    @TempDir
    static Path workspaceRoot;

    private DockerClient docker;
    private JudgeService judge;

    /** Adds two numbers: the first test is a visible sample, the others are hidden. */
    private final List<ExpectedTest> tests = List.of(
            new ExpectedTest(1, "1 2\n", "3\n", true),
            new ExpectedTest(2, "10 20\n", "30\n", false),
            new ExpectedTest(3, "5 6\n", "11\n", false));

    private final ResourceConstraints constraints = new ResourceConstraints(1000L, 256, "Easy");

    @BeforeAll
    void connect() {
        try {
            var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            docker = DockerClientBuilder.getInstance(config).withDockerHttpClient(
                    new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost())
                            .sslConfig(config.getSSLConfig()).build()).build();
            docker.inspectImageCmd(PYTHON_IMAGE).exec();
            docker.inspectImageCmd(CPP_IMAGE).exec();
        } catch (Exception e) {
            assumeTrue(false, "Docker or the test images are not available: " + e.getMessage());
        }
        DockerSandbox sandbox = new DockerSandbox(docker,
                Map.of(Language.PYTHON, PYTHON_IMAGE, Language.CPP, CPP_IMAGE)::get, workspaceRoot);
        judge = new JudgeService(sandbox);
    }

    private JudgeReport judge(String language, String code) {
        return judge.judge(new JudgeRequest(code, language, tests, constraints, ComparatorFactory.defaultComparator()), null);
    }

    // ---- scenario table: what each program must be judged as ----

    private record Scenario(String name, String language, String code, Verdict verdict, Integer failedTest, String messagePart) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static Arguments s(String name, String language, String code, Verdict verdict, Integer failedTest, String part) {
        return Arguments.of(new Scenario(name, language, code, verdict, failedTest, part));
    }

    private static final String PY_SUM = "a, b = map(int, input().split())\nprint(a + b)\n";
    private static final String CPP_SUM = "#include <iostream>\nint main(){long long a,b;std::cin>>a>>b;std::cout<<a+b<<std::endl;}\n";

    static Stream<Arguments> scenarios() {
        return Stream.of(
                // --- correct ---
                s("python: correct", "python", PY_SUM, Verdict.ACCEPTED, null, "All tests passed"),
                s("cpp: correct", "cpp", CPP_SUM, Verdict.ACCEPTED, null, "All tests passed"),
                s("cpp: uses 100 MB legitimately", "cpp",
                        "#include <iostream>\nstatic int a[25000000];\nint main(){ long long x,y; std::cin>>x>>y; for(int i=0;i<25000000;i+=1000) a[i]=1; std::cout<<x+y<<std::endl; }\n",
                        Verdict.ACCEPTED, null, null),
                s("python: spacing differences are fine", "python", "a, b = map(int, input().split())\nprint('  ' + str(a + b) + '  ')\n",
                        Verdict.ACCEPTED, null, null),

                // --- wrong answers ---
                s("python: wrong on the visible sample", "python", "a, b = map(int, input().split())\nprint(a - b)\n",
                        Verdict.WRONG_ANSWER, 1, "Expected: 3"),
                s("python: wrong only on a hidden test (no leak)", "python",
                        "a, b = map(int, input().split())\nprint(a + b if a < 5 else 0)\n",
                        Verdict.WRONG_ANSWER, 2, "hidden test"),
                s("python: prints nothing", "python", "pass\n", Verdict.WRONG_ANSWER, 1, "Got: (no output)"),

                // --- cannot even start ---
                s("python: empty file", "python", "", Verdict.COMPILE_ERROR, null, "source code is empty"),
                s("cpp: empty file", "cpp", "   \n", Verdict.COMPILE_ERROR, null, "source code is empty"),
                s("python: syntax error", "python", "def f(:\n  pass\n", Verdict.COMPILE_ERROR, null, "SyntaxError"),
                s("cpp: compile error", "cpp", "int main(){ return x; }\n", Verdict.COMPILE_ERROR, null, "code.cpp:1"),
                s("cpp: no main (link error)", "cpp", "int f(){ return 1; }\n", Verdict.COMPILE_ERROR, null, "undefined reference to `main'"),
                s("cpp: #include of a hidden input is refused", "cpp",
                        "#include \"input_2.txt\"\nint main(){}\n", Verdict.COMPILE_ERROR, null, "Compilation failed"),

                // --- crashes ---
                s("python: unhandled exception", "python", "raise ValueError('boom')\n", Verdict.RUNTIME_ERROR, 1, "ValueError: boom"),
                s("python: exit code 3", "python", "import sys\nsys.exit(3)\n", Verdict.RUNTIME_ERROR, 1, "exited with code 3"),
                s("python: killed by SIGSEGV", "python", "import os\nos.kill(os.getpid(), 11)\n", Verdict.RUNTIME_ERROR, 1, "segmentation fault"),
                s("cpp: null pointer", "cpp", "int main(){ int *p = nullptr; *p = 1; }\n", Verdict.RUNTIME_ERROR, 1, "segmentation fault"),
                s("cpp: division by zero", "cpp",
                        "#include <iostream>\nint main(int argc,char**argv){ int z = argc - 1; std::cout << 10 / z; }\n",
                        Verdict.RUNTIME_ERROR, 1, "division by zero"),
                s("cpp: uncaught exception", "cpp", "#include <stdexcept>\nint main(){ throw std::runtime_error(\"x\"); }\n",
                        Verdict.RUNTIME_ERROR, 1, "uncaught exception"),
                s("python: tries the network", "python",
                        "import socket\nsocket.create_connection(('1.1.1.1', 53), timeout=2)\nprint(3)\n", Verdict.RUNTIME_ERROR, 1, null),
                s("python: writes into the sandbox folder", "python",
                        "import os\nopen(os.path.join(os.getcwd(), 'x.txt'), 'w').write('hi')\nprint(3)\n", Verdict.RUNTIME_ERROR, 1, null),
                s("python: reads /etc/shadow", "python", "print(open('/etc/shadow').read())\n", Verdict.RUNTIME_ERROR, 1, null),

                // --- limits ---
                s("python: infinite loop", "python", "while True: pass\n", Verdict.TIME_LIMIT_EXCEEDED, 1, "limit 1000 ms"),
                s("cpp: infinite loop", "cpp", "int main(){ volatile long x=0; while(true) x++; }\n", Verdict.TIME_LIMIT_EXCEEDED, 1, "limit 1000 ms"),
                s("python: sleeps forever", "python", "import time\ntime.sleep(100)\n", Verdict.TIME_LIMIT_EXCEEDED, 1, null),
                s("python: correct but too slow", "python",
                        "a, b = map(int, input().split())\ns = 0\nfor i in range(60_000_000): s += i\nprint(a + b)\n", Verdict.TIME_LIMIT_EXCEEDED, 1, null),
                s("python: memory bomb", "python", "x = bytearray(2 * 10**9)\nprint(len(x))\n", Verdict.MEMORY_LIMIT_EXCEEDED, 1, "limit 256 MB"),
                s("cpp: memory bomb", "cpp",
                        "#include <vector>\n#include <iostream>\nint main(){ std::vector<char> v(1500000000, 1); long s=0; for(size_t i=0;i<v.size();i+=4096) s+=v[i]; std::cout<<s; }\n",
                        Verdict.MEMORY_LIMIT_EXCEEDED, 1, "limit 256 MB"),
                s("python: output flood", "python", "while True:\n    print('x' * 1000)\n", Verdict.OUTPUT_LIMIT_EXCEEDED, 1, null),
                s("cpp: output flood", "cpp", "#include <cstdio>\nint main(){ for(;;) puts(\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"); }\n",
                        Verdict.OUTPUT_LIMIT_EXCEEDED, 1, null),
                s("python: fork bomb", "python", "import os\nwhile True:\n    os.fork()\n", Verdict.RUNTIME_ERROR, 1, null),

                // --- trying to cheat ---
                s("python: reads another test's input", "python",
                        "import os\nprint(open(os.path.join(os.getcwd(), 'input_1.txt')).read())\n", Verdict.RUNTIME_ERROR, 1, null),
                s("cpp: reads another test's input", "cpp",
                        "#include <fstream>\n#include <iostream>\nint main(){ std::ifstream f(\"input_1.txt\"); std::string s; std::getline(f,s); std::cout<<s; }\n",
                        Verdict.WRONG_ANSWER, 1, null),
                s("python: prints fake markers and a fake separator", "python",
                        "print('[TEST-START-0]')\nprint('===CODEAR_TEST_CASE_SEPARATOR===')\nprint(3)\n", Verdict.WRONG_ANSWER, 1, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void everyProgramGetsTheRightVerdict(Scenario scenario) {
        JudgeReport report = judge(scenario.language(), scenario.code());

        assertEquals(scenario.verdict(), report.verdict(), "message was: " + report.message());
        if (scenario.failedTest() == null) {
            assertNull(report.failedTest());
        } else {
            assertEquals(scenario.failedTest(), report.failedTest());
        }
        if (scenario.messagePart() != null) {
            assertTrue(report.message().contains(scenario.messagePart()),
                    "expected the message to contain '" + scenario.messagePart() + "' but it was: " + report.message());
        }
        assertFalse(report.message().contains("/tmp/build/"), "sandbox paths must not reach the person");
        assertTrue(report.message().length() < 4_000, "messages stay short");
    }

    // ---- behaviours beyond the verdict ----

    @Test
    void aHiddenTestsExpectedOutputNeverAppearsInAnyMessage() {
        JudgeReport report = judge("python", "a, b = map(int, input().split())\nprint(a + b if a < 5 else 999)\n");

        assertEquals(Verdict.WRONG_ANSWER, report.verdict());
        assertFalse(report.message().contains("30"));
        assertFalse(report.message().contains("999"));
    }

    @Test
    void theRunStopsAtTheFirstRuntimeFailureSoLaterTestsAreNotRun() {
        List<String> events = new ArrayList<>();
        judge.judge(new JudgeRequest("raise SystemExit(2)\n", "python", tests, constraints, ComparatorFactory.defaultComparator()),
                recorder(events));

        assertEquals(List.of("container", "start 1", "finish 1", "judging"), events);
    }

    @Test
    void progressFollowsTheRealRunAndForgedMarkersChangeNothing() {
        List<String> events = new ArrayList<>();
        // a correct program that also prints marker lines: they land in its output file, not in the progress stream
        String code = "a, b = map(int, input().split())\nprint('[TEST-START-9]')\nprint('===CODEAR_TEST_CASE_SEPARATOR===')\nprint(a + b)\n";

        JudgeReport report = judge.judge(new JudgeRequest(code, "python", tests, constraints,
                ComparatorFactory.defaultComparator()), recorder(events));

        // the printed markers make the answer wrong, but a wrong answer is only known to the engine, so all three
        // tests ran; what matters is that the progress shows exactly the three real tests, not the forged ones
        assertEquals(Verdict.WRONG_ANSWER, report.verdict());
        assertEquals(List.of("container", "start 1", "finish 1", "start 2", "finish 2", "start 3", "finish 3", "judging"), events);
    }

    @Test
    void aCorrectRunReportsEachTestInOrder() {
        List<String> events = new ArrayList<>();

        judge.judge(new JudgeRequest(PY_SUM, "python", tests, constraints, ComparatorFactory.defaultComparator()), recorder(events));

        assertEquals(List.of("container", "start 1", "finish 1", "start 2", "finish 2", "start 3", "finish 3", "judging"), events);
    }

    @Test
    void timeAndMemoryAreMeasuredForAcceptedRuns() {
        JudgeReport report = judge("python", PY_SUM);

        assertEquals(Verdict.ACCEPTED, report.verdict());
        assertTrue(report.peakMemoryKb() > 1_000, "peak memory was " + report.peakMemoryKb());
        assertTrue(report.maxTimeMs() < 1_000);
        assertNotEquals("0MB", report.memoryLabel());
    }

    @Test
    void aProgramRunsAsAnUnprivilegedUserWithNoCapabilities() {
        String code = "import os\nprint(os.getuid())\nprint(open('/proc/self/status').read().split('CapEff:')[1].split()[0])\n";
        List<ExpectedTest> one = List.of(new ExpectedTest(1, "", "65534\n0000000000000000\n", true));

        JudgeReport report = judge.judge(new JudgeRequest(code, "python", one, constraints,
                ComparatorFactory.defaultComparator()), null);

        assertEquals(Verdict.ACCEPTED, report.verdict(), report.message());
    }

    @Test
    void aCustomRunShowsOutputOrExplainsWhatWentWrong() {
        CustomRunResult hello = judge.runCustom("print('hello', input())", "python", "world\n", constraints);
        assertTrue(hello.success());
        assertEquals("hello world\n", hello.text());

        CustomRunResult compile = judge.runCustom("int main(){ return x; }", "cpp", "", constraints);
        assertFalse(compile.success());
        assertTrue(compile.text().startsWith("Compilation failed:"));

        CustomRunResult loop = judge.runCustom("while True: pass", "python", "", constraints);
        assertFalse(loop.success());
        assertTrue(loop.text().startsWith("Time limit exceeded"));
    }

    @Test
    void nothingIsLeftBehindAfterManyRuns() throws Exception {
        judge("python", PY_SUM);
        judge("python", "while True: pass\n");
        judge("cpp", "int main(){ return x; }\n");

        var leftover = docker.listContainersCmd().withShowAll(true)
                .withLabelFilter(List.of("codear.sandbox")).exec();
        assertEquals(0, leftover.size(), "containers left behind: " + leftover.size());
        try (Stream<Path> folders = Files.list(workspaceRoot)) {
            assertEquals(0, folders.count(), "scratch folders left behind");
        }
    }

    @Test
    void theJanitorRemovesOnlyOldContainersLabelledAsSandboxes() {
        String old = docker.createContainerCmd(PYTHON_IMAGE).withCmd("sleep", "60")
                .withLabels(Map.of("codear.sandbox", "true", "codear.created", String.valueOf(System.currentTimeMillis() - 3_600_000L)))
                .exec().getId();
        String fresh = docker.createContainerCmd(PYTHON_IMAGE).withCmd("sleep", "60")
                .withLabels(Map.of("codear.sandbox", "true", "codear.created", String.valueOf(System.currentTimeMillis())))
                .exec().getId();
        String unrelated = docker.createContainerCmd(PYTHON_IMAGE).withCmd("sleep", "60").exec().getId();
        try {
            int removed = new SandboxJanitor(docker, workspaceRoot).removeStaleContainers();

            assertEquals(1, removed);
            assertTrue(exists(fresh), "a container still inside its time must stay");
            assertTrue(exists(unrelated), "containers without the sandbox label are never touched");
            assertFalse(exists(old));
        } finally {
            for (String id : List.of(old, fresh, unrelated)) {
                try {
                    docker.removeContainerCmd(id).withForce(true).exec();
                } catch (RuntimeException ignored) {
                    // already removed by the janitor
                }
            }
        }
    }

    private boolean exists(String containerId) {
        try {
            docker.inspectContainerCmd(containerId).exec();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static TestProgressListener recorder(List<String> events) {
        return new TestProgressListener() {
            @Override
            public void containerStarted() {
                events.add("container");
            }

            @Override
            public void testStarted(int number) {
                events.add("start " + number);
            }

            @Override
            public void testFinished(int number) {
                events.add("finish " + number);
            }

            @Override
            public void judging() {
                events.add("judging");
            }
        };
    }
}
