package com.codear.engine.sandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.util.FileSystemUtils;

import com.codear.engine.constants.SandboxProtocol;

/**
 * A scratch folder on the engine's disk holding one submission's source, its test inputs and the runner script.
 * It is copied into the container, and deleted when this is closed.
 */
public final class SandboxWorkspace implements AutoCloseable {

    private static final String PREFIX = "codear_";

    private final Path directory;

    private SandboxWorkspace(Path directory) {
        this.directory = directory;
    }

    public static SandboxWorkspace create(Path root, Language language, String code, List<String> inputs)
            throws IOException {
        Files.createDirectories(root);
        Path directory = Files.createTempDirectory(root, PREFIX);
        SandboxWorkspace workspace = new SandboxWorkspace(directory);
        try {
            // the docker daemon may run as another user, so the folder has to be readable by everyone
            makeWorldReadable(directory);
            Files.writeString(directory.resolve(language.sourceFile()), code);
            Files.writeString(directory.resolve(SandboxProtocol.RUNNER_FILE), RunnerScript.contents());
            for (int i = 0; i < inputs.size(); i++) {
                Files.writeString(directory.resolve(SandboxProtocol.inputFile(i)), inputs.get(i));
            }
            return workspace;
        } catch (IOException | RuntimeException e) {
            workspace.close();
            throw e;
        }
    }

    public Path directory() {
        return directory;
    }

    public String folderName() {
        return directory.getFileName().toString();
    }

    /** Whether a directory name was made by this class, so a janitor knows what it may delete. */
    public static boolean isWorkspaceName(String name) {
        return name != null && name.startsWith(PREFIX);
    }

    @Override
    public void close() {
        FileSystemUtils.deleteRecursively(directory.toFile());
    }

    private static void makeWorldReadable(Path path) {
        path.toFile().setReadable(true, false);
        path.toFile().setExecutable(true, false);
    }
}
