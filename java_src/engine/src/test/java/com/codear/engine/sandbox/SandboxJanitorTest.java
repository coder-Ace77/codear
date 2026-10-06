package com.codear.engine.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SandboxJanitorTest {

    private static Path folder(Path root, String name, Duration age) throws IOException {
        Path folder = Files.createDirectory(root.resolve(name));
        Files.writeString(folder.resolve("code.py"), "print(1)");
        assertTrue(folder.toFile().setLastModified(System.currentTimeMillis() - age.toMillis()));
        return folder;
    }

    @Test
    void removesOldScratchFoldersMadeByTheEngineAndNothingElse(@TempDir Path root) throws IOException {
        Path oldOurs = folder(root, "codear_old", Duration.ofHours(2));
        Path freshOurs = folder(root, "codear_fresh", Duration.ofMinutes(1));
        Path oldNotOurs = folder(root, "somebody-elses-data", Duration.ofDays(30));

        int removed = new SandboxJanitor(null, root).removeStaleWorkspaces();

        assertEquals(1, removed);
        assertFalse(Files.exists(oldOurs));
        assertTrue(Files.exists(freshOurs), "a run in progress must not be deleted");
        assertTrue(Files.exists(oldNotOurs), "only folders with the engine's prefix may ever be touched");
    }

    @Test
    void aMissingScratchRootIsNotAnError(@TempDir Path root) {
        assertEquals(0, new SandboxJanitor(null, root.resolve("does-not-exist")).removeStaleWorkspaces());
    }
}
