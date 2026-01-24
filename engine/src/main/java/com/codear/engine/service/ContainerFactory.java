package com.codear.engine.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.codear.engine.constants.LanguageConstants;
import com.codear.engine.entity.LanguageConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.codear.engine.dto.CodeExecutionResult;

@Component
public class ContainerFactory implements AutoCloseable {

    public static final String OUTPUT_SEPARATOR = "===CODEAR_TEST_CASE_SEPARATOR===";

    private final DockerHttpClient httpClient;
    private final DockerClient dockerClient;

    public ContainerFactory() {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        this.httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        this.dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();

        try {
            pullImage(LanguageConstants.PYTHON_DOCKER_IMAGE);
            pullImage(LanguageConstants.CPP_DOCKER_IMAGE);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    public Path createTempCodeFiles(String code, List<String> inputs, String codeFileName) throws IOException {
        Path executionRoot = Path.of(System.getProperty("user.home"), "codear_executions");
        if (!Files.exists(executionRoot)) {
            Files.createDirectories(executionRoot);
        }
        Path tempDir = Files.createTempDirectory(executionRoot, "codear_");
        tempDir.toFile().setReadable(true, false);
        tempDir.toFile().setWritable(true, false);
        tempDir.toFile().setExecutable(true, false);
        Files.writeString(tempDir.resolve(codeFileName), code);
        for (int i = 0; i < inputs.size(); i++) {
            String inputFileName = "input_" + i + ".txt";
            Path inputPath = tempDir.resolve(inputFileName);
            Files.writeString(inputPath, inputs.get(i));
            inputPath.toFile().setReadable(true, false);
        }
        return tempDir;
    }

    public void pullImage(String image) throws InterruptedException {
        if (image.startsWith("codear-")) {
            return;
        }
        dockerClient.pullImageCmd(image)
                .exec(new PullImageResultCallback())
                .awaitCompletion(5, TimeUnit.MINUTES);
    }

    public String createContainer(LanguageConfig config, Path tempDir, Integer memoryLimitMb, int numTestCases) {
        String compileString = (config.getCompileCmd() != null)
                ? config.getCompileCmd().stream()
                        .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                        .collect(Collectors.joining(" "))
                : null;
        String runString = config.getRunCmd().stream()
                .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                .collect(Collectors.joining(" "));

        long memoryInBytes = (memoryLimitMb != null ? memoryLimitMb : 256) * 1024L * 1024L;

        String script;
        String timeCmd = "/usr/bin/time -f \"METRICS:%e:%M\"";

        // IMPORTANT: Inputs are now local to working directory, so "input_$i.txt"
        // instead
        // of "/app/input_$i.txt"
        // Also assuming we copy 'tempDir' to '/app', so the dir name inside will be
        // 'codear_xxxx'
        // So WorkingDir should be /app/codear_xxxx

        if (compileString != null) {
            script = String.format(
                    "%s; " +
                            "if [ $? -eq 0 ]; then " +
                            "    /bin/sh -c 'for i in $(seq 0 $(($NUM_TESTS - 1))); do " +
                            "        echo \"[TEST-START-$i]\"; " +
                            "        echo \"[TEST-OUTPUT-START]\"; " +
                            "        %s %s < input_$i.txt; " +
                            "        EXIT_CODE=$?; " +
                            "        echo \"\"; " +
                            "        echo \"[TEST-OUTPUT-END]\"; " +
                            "        if [ $EXIT_CODE -ne 0 ]; then echo \"[TEST-FAILED-$i-CODE-$EXIT_CODE]\"; fi; "
                            +
                            "        echo \"%s\"; " +
                            "    done'; " +
                            "fi",
                    compileString, timeCmd, runString, OUTPUT_SEPARATOR);
        } else {
            script = String.format(
                    "/bin/sh -c 'for i in $(seq 0 $(($NUM_TESTS - 1))); do " +
                            "    echo \"[TEST-START-$i]\"; " +
                            "    echo \"[TEST-OUTPUT-START]\"; " +
                            "    %s %s < input_$i.txt; " +
                            "    EXIT_CODE=$?; " +
                            "    echo \"\"; " +
                            "    echo \"[TEST-OUTPUT-END]\"; " +
                            "    if [ $EXIT_CODE -ne 0 ]; then echo \"[TEST-FAILED-$i-CODE-$EXIT_CODE]\"; fi; " +
                            "    echo \"%s\"; " +
                            "    done'",
                    timeCmd, runString, OUTPUT_SEPARATOR);
        }

        String[] shellCmd = { "/bin/sh", "-c", script };

        // Construct working directory path: /app/codear_...
        String folderName = tempDir.getFileName().toString();
        String containerWorkDir = "/app/" + folderName;

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(memoryInBytes);
        // No binds!

        CreateContainerResponse container = dockerClient.createContainerCmd(config.getImage())
                .withCmd(shellCmd)
                .withHostConfig(hostConfig)
                .withEnv("NUM_TESTS=" + numTestCases)
                .withWorkingDir(containerWorkDir)
                .exec();

        // COPY files to container
        dockerClient.copyArchiveToContainerCmd(container.getId()).withHostResource(tempDir.toString())
                .withRemotePath("/app").exec();

        return container.getId();
    }

    public CodeExecutionResult runContainerAndGetLogs(String containerId, int numTestCases, long timeLimitPerTestMs)
            throws InterruptedException {
        dockerClient.startContainerCmd(containerId).exec();

        final StringBuilder logs = new StringBuilder();
        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame frame) {
                if (frame != null) {
                    logs.append(new String(Objects.requireNonNull(frame.getPayload())));
                }
            }
        };

        long totalTimeout = (numTestCases * timeLimitPerTestMs) + 20000;

        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(callback)
                .awaitCompletion(totalTimeout, TimeUnit.MILLISECONDS);

        String rawLogs = logs.toString();
        Pattern pattern = Pattern.compile("METRICS:([\\d\\.]+):(\\d+)");
        Matcher matcher = pattern.matcher(rawLogs);

        long totalCpuTimeMs = 0L;
        long maxMemoryKb = 0L;

        while (matcher.find()) {
            try {
                double seconds = Double.parseDouble(matcher.group(1));
                totalCpuTimeMs += (long) (seconds * 1000);

                long kb = Long.parseLong(matcher.group(2));
                if (kb > maxMemoryKb) {
                    maxMemoryKb = kb;
                }
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse metrics: " + e.getMessage());
            }
        }

        String memoryUsed = String.format("%.2fMB", maxMemoryKb / 1024.0);

        return CodeExecutionResult.builder()
                .logs(rawLogs)
                .cpuTimeMs(totalCpuTimeMs)
                .memoryUsedPk(memoryUsed)
                .build();
    }

    public void cleanupContainer(String containerId) {
        if (containerId != null) {
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void cleanupTempDirectory(Path tempDir) {
        if (tempDir != null) {
            try {
                try (var stream = Files.walk(tempDir)) {
                    stream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (dockerClient != null) {
            dockerClient.close();
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }
}