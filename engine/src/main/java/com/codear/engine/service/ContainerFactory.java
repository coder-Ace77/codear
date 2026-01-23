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

import org.springframework.util.StopWatch;

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
        System.out.println("[ContainerFactory] Image warmup complete.");
    }

    // public Path createTempCodeFiles(String code, List<String> inputs, String
    // codeFileName) throws IOException {

    // Path tempDir = Files.createTempDirectory("codear_");

    // Files.writeString(tempDir.resolve(codeFileName), code);

    // for (int i = 0; i < inputs.size(); i++) {
    // String inputFileName = "input_" + i + ".txt";
    // Files.writeString(tempDir.resolve(inputFileName), inputs.get(i));
    // }

    // return tempDir;
    // }

    public Path createTempCodeFiles(String code, List<String> inputs, String codeFileName) throws IOException {
        StopWatch stopWatch = new StopWatch("ContainerFactory.createTempCodeFiles");
        try {
            stopWatch.start("check-root-tmp");
            // Force use of /tmp specifically
            Path rootTmp = Path.of("/tmp");
            if (!Files.exists(rootTmp)) {
                Files.createDirectories(rootTmp);
            }
            stopWatch.stop();

            stopWatch.start("create-dir");
            // Create the directory specifically in /tmp
            Path tempDir = Files.createTempDirectory(rootTmp, "codear_");

            // Set permissions so the Docker daemon (running as a different user) can read
            // it
            tempDir.toFile().setReadable(true, false);
            tempDir.toFile().setExecutable(true, false);
            stopWatch.stop();

            stopWatch.start("write-code-file");
            Files.writeString(tempDir.resolve(codeFileName), code);
            stopWatch.stop();

            stopWatch.start("write-inputs");
            for (int i = 0; i < inputs.size(); i++) {
                String inputFileName = "input_" + i + ".txt";
                Path inputPath = tempDir.resolve(inputFileName);
                Files.writeString(inputPath, inputs.get(i));
                // Ensure the file is readable
                inputPath.toFile().setReadable(true, false);
            }
            stopWatch.stop();

            System.out.println("[DEBUG] Created temp files at: " + tempDir.toAbsolutePath());
            return tempDir;
        } finally {
            if (stopWatch.isRunning())
                stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

    public void pullImage(String image) throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("ContainerFactory.pullImage");
        try {
            dockerClient.pullImageCmd(image)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion(5, TimeUnit.MINUTES);
            System.out.println("[PULL_IMAGE] Image pull complete.");
        } finally {
            stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

    public String createContainer(LanguageConfig config, Path tempDir, Integer memoryLimitMb, int numTestCases) {
        StopWatch stopWatch = new StopWatch("ContainerFactory.createContainer");
        try {
            stopWatch.start("prepare-config");
            Volume volume = new Volume("/app");
            String compileString = (config.getCompileCmd() != null)
                    ? String.join(" ", config.getCompileCmd())
                    : null;
            String runString = String.join(" ", config.getRunCmd());

            // --- DEBUGGING: List files inside the worker container before running code ---

            String script;
            if (compileString != null) {
                script = String.format(
                        "%s; " +
                                "if [ $? -eq 0 ]; then " +
                                "    for i in $(seq 0 $(($NUM_TESTS - 1))); do " +
                                "        %s < /app/input_$i.txt; " +
                                "        echo '%s'; " +
                                "    done; " +
                                "fi",
                        compileString, runString, OUTPUT_SEPARATOR);
            } else {
                script = String.format(
                        "for i in $(seq 0 $(($NUM_TESTS - 1))); do " +
                                "    %s < /app/input_$i.txt; " +
                                "    echo '%s'; " +
                                "done",
                        runString, OUTPUT_SEPARATOR);
            }

            String[] shellCmd = { "/bin/sh", "-c", script };

            // --- PATH LOGGING ---
            String absolutePath = tempDir.toAbsolutePath().toString();
            System.out.println("[DEBUG] Host Path to bind: " + absolutePath);

            // Ensure the directory is actually readable/traversable by Docker
            tempDir.toFile().setReadable(true, false);
            tempDir.toFile().setExecutable(true, false);

            Bind bind = new Bind(absolutePath, volume);

            long memoryInBytes = memoryLimitMb * 1024L * 1024L;
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(memoryInBytes)
                    .withBinds(bind);
            stopWatch.stop();

            stopWatch.start("docker-api-create");
            CreateContainerResponse container = dockerClient.createContainerCmd(config.getImage())
                    .withCmd(shellCmd)
                    .withHostConfig(hostConfig)
                    .withEnv("NUM_TESTS=" + numTestCases)
                    .exec();
            stopWatch.stop();

            return container.getId();
        } finally {
            if (stopWatch.isRunning())
                stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

    public String runContainerAndGetLogs(String containerId, long totalTimeLimitMs) throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("ContainerFactory.runContainerAndGetLogs");
        try {
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

            long timeoutWithBuffer = totalTimeLimitMs + 20000;
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(callback)
                    .awaitCompletion(timeoutWithBuffer, TimeUnit.MILLISECONDS); // Use dynamic timeout
            return logs.toString();
        } finally {
            stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

    public void cleanupContainer(String containerId) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("ContainerFactory.cleanupContainer");
        try {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
        }
    }

    public void cleanupTempDirectory(Path tempDir) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("ContainerFactory.cleanupTempDirectory");
        try {
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
        } finally {
            stopWatch.stop();
            System.out.println(stopWatch.prettyPrint());
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