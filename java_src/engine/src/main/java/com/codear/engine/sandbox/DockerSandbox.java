package com.codear.engine.sandbox;

import static com.codear.engine.constants.JudgeDefaults.SANDBOX_REMOTE_ROOT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.codear.engine.constants.SandboxProtocol;
import com.codear.engine.dto.judge.JudgeLimits;
import com.codear.engine.dto.sandbox.SandboxResult;
import com.codear.engine.enums.Language;
import com.codear.engine.exception.SandboxException;
import com.codear.engine.util.sandbox.ContainerSpec;
import com.codear.engine.util.sandbox.ResultArchiveReader;
import com.codear.engine.util.sandbox.SandboxWorkspace;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DockerSandbox implements SandboxRunner {

    private final DockerClient docker;
    private final Map<Language, String> images;
    private final Path workspaceRoot;
    private final ContainerSpec spec = new ContainerSpec();
    private final ResultArchiveReader archiveReader = new ResultArchiveReader();

    public DockerSandbox(DockerClient docker, String pythonImage, String cppImage, Path workspaceRoot) {
        this.docker = docker;
        this.images = Map.of(Language.PYTHON, pythonImage, Language.CPP, cppImage);
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public SandboxResult run(Language language, String code, List<String> inputs, JudgeLimits limits) {
        int numTests = inputs.size();

        // creating a folder with files
        try (SandboxWorkspace workspace = SandboxWorkspace.createWorkSpaceWithFiles(workspaceRoot, language, code, inputs)) {

            // calls docker client to create container
            String containerId = createContainer(language, numTests, limits, workspace);
            try {
                copyWorkspaceInContainer(containerId, workspace);
                startContainerAndWait(containerId, numTests, limits);
                boolean oomKilled = wasOomKilled(containerId);
                SandboxResult result = copyOutFromContainerAsTar(containerId, workspace, numTests, limits, oomKilled);
                return result;
            } finally {
                killAndRemoveContainer(containerId);
            }
        } catch (IOException | DockerException e) {
            throw new SandboxException("Sandbox run failed: " + e.getMessage(), e);
        }
    }

    private String createContainer(Language language, int numTests, JudgeLimits limits, SandboxWorkspace workspace) {
        String remoteDir = remoteDir(workspace);
        return docker.createContainerCmd(images.get(language))
                .withCmd("sh", remoteDir + "/" + SandboxProtocol.RUNNER_FILE)
                .withEnv(spec.environment(language, numTests, limits))
                .withWorkingDir(remoteDir)
                .withHostConfig(spec.hostConfig(limits))
                .withLabels(spec.labels(System.currentTimeMillis()))
                .exec()
                .getId();
    }

    private void copyWorkspaceInContainer(String containerId, SandboxWorkspace workspace) {
        docker.copyArchiveToContainerCmd(containerId)
                .withHostResource(workspace.directory().toString())
                .withRemotePath(SANDBOX_REMOTE_ROOT)
                .exec();
    }

    private boolean startContainerAndWait(String containerId, int numTests, JudgeLimits limits) {
        docker.startContainerCmd(containerId).exec();

        long timeoutSeconds = limits.totalTimeoutSeconds(numTests);
        try {
            docker.waitContainerCmd(containerId).exec(new WaitContainerResultCallback())
                    .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);
            return true;
        } catch (DockerClientException timedOut) {
            killContainer(containerId);
            return false;
        }
    }

    private boolean wasOomKilled(String containerId) {
        try {
            Boolean killed = docker.inspectContainerCmd(containerId).exec().getState().getOOMKilled();
            return Boolean.TRUE.equals(killed);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private SandboxResult copyOutFromContainerAsTar(String containerId, SandboxWorkspace workspace, int numTests,
            JudgeLimits limits, boolean oomKilled) {
        String resultsPath = remoteDir(workspace) + "/" + SandboxProtocol.RESULTS_DIR + "/.";
        long maxFileBytes = limits.getOutputLimitBytes() + 1024;
        try (InputStream archive = docker.copyArchiveFromContainerCmd(containerId, resultsPath).exec()) {
            return archiveReader.read(archive, numTests, maxFileBytes, oomKilled);
        } catch (IOException | DockerException e) {
            throw new SandboxException("Could not read the sandbox results: " + e.getMessage(), e);
        }
    }

    private static String remoteDir(SandboxWorkspace workspace) {
        return SANDBOX_REMOTE_ROOT + "/" + workspace.folderName();
    }

    private void killContainer(String containerId) {
        try {
            docker.killContainerCmd(containerId).exec();
        } catch (RuntimeException ignored) {
            // already stopped
        }
    }

    private void killAndRemoveContainer(String containerId) {
        try {
            docker.removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).exec();
        } catch (RuntimeException e) {
            log.warn("Could not remove sandbox {}: {}", getContainerShortId(containerId), e.getMessage());
        }
    }

    private static String getContainerShortId(String containerId) {
        return containerId.substring(0, Math.min(12, containerId.length()));
    }
}
