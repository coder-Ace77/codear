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

    public Path createTempCodeFiles(String code, List<String> inputs, String codeFileName) throws IOException {
        Path tempDir = Files.createTempDirectory("codear_");
        

        Files.writeString(tempDir.resolve(codeFileName), code);

        for (int i = 0; i < inputs.size(); i++) {
            String inputFileName = "input_" + i + ".txt";
            Files.writeString(tempDir.resolve(inputFileName), inputs.get(i));
        }

        return tempDir;
    }

    public void pullImage(String image) throws InterruptedException {
        dockerClient.pullImageCmd(image)
                .exec(new PullImageResultCallback())
                .awaitCompletion(5, TimeUnit.MINUTES);
        System.out.println("[PULL_IMAGE] Image pull complete.");
    }

    
    public String createContainer(LanguageConfig config, Path tempDir, Integer memoryLimitMb, int numTestCases) {
        Volume volume = new Volume("/app");
        String compileString = (config.getCompileCmd() != null) 
                               ? String.join(" ", config.getCompileCmd()) 
                               : null;
        String runString = String.join(" ", config.getRunCmd());

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
                compileString, runString, OUTPUT_SEPARATOR
            );
        } else {
            script = String.format(
                "for i in $(seq 0 $(($NUM_TESTS - 1))); do " +
                "    %s < /app/input_$i.txt; " +  
                "    echo '%s'; " +
                "done",
                runString, OUTPUT_SEPARATOR
            );
        }


        String[] shellCmd = {
            "/bin/sh",
            "-c",
            script
        };


        Bind bind = new Bind(tempDir.toAbsolutePath().toString(), volume);

        long memoryInBytes = memoryLimitMb * 1024L * 1024L;
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(memoryInBytes)
                .withBinds(bind); 
        
        CreateContainerResponse container = dockerClient.createContainerCmd(config.getImage())
                .withCmd(shellCmd) 
                .withHostConfig(hostConfig) 
                .withEnv("NUM_TESTS=" + numTestCases)
                .exec();
        
        String containerId = container.getId();
        return containerId;
    }

    public String runContainerAndGetLogs(String containerId, long totalTimeLimitMs) throws InterruptedException {
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