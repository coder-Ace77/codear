package com.codear.engine.sandbox;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

/** Wires the Docker client, the sandbox and its janitor. Image names and the scratch folder can be overridden. */
@Configuration
@EnableScheduling
public class SandboxConfig {

    @Bean(destroyMethod = "close")
    public DockerClient dockerClient() {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        return DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();
    }

    @Bean
    public Path sandboxWorkspaceRoot(@Value("${judge.workspace-root:}") String configured) {
        return configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "codear_executions")
                : Path.of(configured);
    }

    @Bean
    public SandboxRunner sandboxRunner(DockerClient docker, Path sandboxWorkspaceRoot,
            @Value("${judge.image.python:codear-python:latest}") String pythonImage,
            @Value("${judge.image.cpp:codear-cpp:latest}") String cppImage) {
        Map<Language, String> images = Map.of(Language.PYTHON, pythonImage, Language.CPP, cppImage);
        return new DockerSandbox(docker, images::get, sandboxWorkspaceRoot);
    }

    @Bean
    public SandboxJanitor sandboxJanitor(DockerClient docker, Path sandboxWorkspaceRoot) {
        return new SandboxJanitor(docker, sandboxWorkspaceRoot);
    }
}
