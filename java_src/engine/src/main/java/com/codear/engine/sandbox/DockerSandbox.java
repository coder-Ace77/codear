package com.codear.engine.sandbox;

import static com.codear.engine.constants.JudgeDefaults.SANDBOX_REMOTE_ROOT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.codear.engine.constants.SandboxProtocol;
import com.codear.engine.service.TestProgressListener;
import com.codear.engine.service.TestProgressParser;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Frame;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs a submission in a short-lived Docker container: copy the workspace in, let the runner script do its work,
 * copy its results folder out, and always remove the container.
 */
@Slf4j
public class DockerSandbox implements SandboxRunner {

    private static final int LOG_FLUSH_SECONDS = 5;

    private final DockerClient docker;
    private final Function<Language, String> imageFor;
    private final Path workspaceRoot;
    private final ContainerSpecFactory spec = new ContainerSpecFactory();
    private final ResultArchiveReader archiveReader = new ResultArchiveReader();

    public DockerSandbox(DockerClient docker, Function<Language, String> imageFor, Path workspaceRoot) {
        this.docker = docker;
        this.imageFor = imageFor;
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public SandboxResult run(SandboxRequest request, TestProgressListener progress) {
        try (SandboxWorkspace workspace = SandboxWorkspace.create(workspaceRoot, request.language(),
                request.code(), request.inputs())) {
            String containerId = createContainer(request, workspace);
            try {
                copyIn(containerId, workspace);
                boolean exitedByItself = startAndWait(containerId, request, progress);
                boolean oomKilled = wasOomKilled(containerId);
                SandboxResult result = copyResultsOut(containerId, workspace, request, oomKilled);
                if (!exitedByItself) {
                    log.warn("Sandbox {} was cut off by the overall timeout", shortId(containerId));
                }
                return result;
            } finally {
                removeQuietly(containerId);
            }
        } catch (IOException | DockerException e) {
            throw new SandboxException("Sandbox run failed: " + e.getMessage(), e);
        }
    }

    private String createContainer(SandboxRequest request, SandboxWorkspace workspace) {
        String remoteDir = remoteDir(workspace);
        return docker.createContainerCmd(imageFor.apply(request.language()))
                .withCmd("sh", remoteDir + "/" + SandboxProtocol.RUNNER_FILE)
                .withEnv(spec.environment(request))
                .withWorkingDir(remoteDir)
                .withHostConfig(spec.hostConfig(request.limits()))
                .withLabels(spec.labels(System.currentTimeMillis()))
                .exec()
                .getId();
    }

    private void copyIn(String containerId, SandboxWorkspace workspace) {
        docker.copyArchiveToContainerCmd(containerId)
                .withHostResource(workspace.directory().toString())
                .withRemotePath(SANDBOX_REMOTE_ROOT)
                .exec();
    }

    /** @return true when the container finished by itself, false when the overall timeout cut it off */
    private boolean startAndWait(String containerId, SandboxRequest request, TestProgressListener progress) {
        TestProgressParser parser = progress == null ? null : new TestProgressParser(progress);
        ResultCallback.Adapter<Frame> logs = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame frame) {
                feed(parser, frame);
            }
        };

        docker.startContainerCmd(containerId).exec();
        if (progress != null) {
            progress.containerStarted();
        }
        docker.logContainerCmd(containerId).withStdOut(true).withStdErr(true).withFollowStream(true).exec(logs);

        long timeoutSeconds = request.limits().totalTimeoutSeconds(request.inputs().size());
        try {
            docker.waitContainerCmd(containerId).exec(new WaitContainerResultCallback())
                    .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);
            return true;
        } catch (DockerClientException timedOut) {
            killQuietly(containerId);
            return false;
        } finally {
            closeLogs(logs);
        }
    }

    private static void feed(TestProgressParser parser, Frame frame) {
        if (parser == null || frame == null || frame.getPayload() == null) {
            return;
        }
        try {
            parser.feed(new String(frame.getPayload(), StandardCharsets.UTF_8));
        } catch (RuntimeException ignored) {
            // progress is cosmetic: it must never disturb a run
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

    private SandboxResult copyResultsOut(String containerId, SandboxWorkspace workspace, SandboxRequest request,
            boolean oomKilled) {
        String resultsPath = remoteDir(workspace) + "/" + SandboxProtocol.RESULTS_DIR + "/.";
        long maxFileBytes = request.limits().outputLimitBytes() + 1024;
        try (InputStream archive = docker.copyArchiveFromContainerCmd(containerId, resultsPath).exec()) {
            return archiveReader.read(archive, request.inputs().size(), maxFileBytes, oomKilled);
        } catch (IOException | DockerException e) {
            throw new SandboxException("Could not read the sandbox results: " + e.getMessage(), e);
        }
    }

    private static String remoteDir(SandboxWorkspace workspace) {
        return SANDBOX_REMOTE_ROOT + "/" + workspace.folderName();
    }

    private void killQuietly(String containerId) {
        try {
            docker.killContainerCmd(containerId).exec();
        } catch (RuntimeException ignored) {
            // already stopped
        }
    }

    private void removeQuietly(String containerId) {
        try {
            docker.removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).exec();
        } catch (RuntimeException e) {
            log.warn("Could not remove sandbox {}: {}", shortId(containerId), e.getMessage());
        }
    }

    private static void closeLogs(ResultCallback.Adapter<Frame> logs) {
        try {
            logs.awaitCompletion(LOG_FLUSH_SECONDS, TimeUnit.SECONDS);
            logs.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // nothing more to read
        }
    }

    private static String shortId(String containerId) {
        return containerId.substring(0, Math.min(12, containerId.length()));
    }
}
