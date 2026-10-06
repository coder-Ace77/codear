package com.codear.engine.sandbox;

import static com.codear.engine.constants.JudgeDefaults.JANITOR_INTERVAL_MS;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_CREATED_LABEL;
import static com.codear.engine.constants.JudgeDefaults.SANDBOX_LABEL;
import static com.codear.engine.constants.JudgeDefaults.STALE_CONTAINER_MINUTES;
import static com.codear.engine.constants.JudgeDefaults.STALE_WORKSPACE_MINUTES;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.FileSystemUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

import lombok.extern.slf4j.Slf4j;

/**
 * Cleans up after crashes: sandbox containers and scratch folders that were never removed because the engine died
 * mid-run. Only things this engine created are touched (a label on containers, a name prefix on folders).
 */
@Slf4j
public class SandboxJanitor {

    private final DockerClient docker;
    private final Path workspaceRoot;

    public SandboxJanitor(DockerClient docker, Path workspaceRoot) {
        this.docker = docker;
        this.workspaceRoot = workspaceRoot;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = JANITOR_INTERVAL_MS)
    public void sweep() {
        try {
            int containers = removeStaleContainers();
            int folders = removeStaleWorkspaces();
            if (containers + folders > 0) {
                log.info("Janitor removed {} stale sandbox container(s) and {} scratch folder(s)", containers, folders);
            }
        } catch (RuntimeException e) {
            log.warn("Janitor sweep failed: {}", e.getMessage());
        }
    }

    int removeStaleContainers() {
        long cutoff = System.currentTimeMillis() - Duration.ofMinutes(STALE_CONTAINER_MINUTES).toMillis();
        List<Container> containers = docker.listContainersCmd().withShowAll(true)
                .withLabelFilter(List.of(SANDBOX_LABEL)).exec();
        int removed = 0;
        for (Container container : containers) {
            if (createdAtMillis(container) < cutoff) {
                docker.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
                removed++;
            }
        }
        return removed;
    }

    int removeStaleWorkspaces() {
        if (!Files.isDirectory(workspaceRoot)) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - Duration.ofMinutes(STALE_WORKSPACE_MINUTES).toMillis();
        int removed = 0;
        try (Stream<Path> entries = Files.list(workspaceRoot)) {
            for (Path entry : (Iterable<Path>) entries::iterator) {
                boolean ours = SandboxWorkspace.isWorkspaceName(entry.getFileName().toString());
                if (ours && entry.toFile().lastModified() < cutoff && FileSystemUtils.deleteRecursively(entry.toFile())) {
                    removed++;
                }
            }
        } catch (IOException e) {
            log.warn("Could not list {}: {}", workspaceRoot, e.getMessage());
        }
        return removed;
    }

    private static long createdAtMillis(Container container) {
        String label = container.getLabels() == null ? null : container.getLabels().get(SANDBOX_CREATED_LABEL);
        try {
            return label == null ? container.getCreated() * 1000 : Long.parseLong(label);
        } catch (NumberFormatException e) {
            return container.getCreated() * 1000;
        }
    }
}
