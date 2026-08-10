package com.eashell.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptRepositoryTest {

    @Test
    void addPersistsAndReloadsAcrossInstances(@TempDir Path tempDir) {
        Path dataFile = tempDir.resolve("eashell_data.json");
        Path legacyFile = tempDir.resolve("legacy_unused.json");

        ScriptRepository repository = new ScriptRepository(dataFile, legacyFile);
        repository.add(new ScriptEntry("build", "C:\\project", List.of("npm install")));

        assertTrue(Files.exists(dataFile));

        List<ScriptEntry> reloaded = new ScriptRepository(dataFile, legacyFile).getAll();
        assertEquals(1, reloaded.size());
        assertEquals("build", reloaded.get(0).getName());
        assertEquals(List.of("npm install"), reloaded.get(0).getCommands());
    }

    @Test
    void updateAndRemoveArePersisted(@TempDir Path tempDir) {
        Path dataFile = tempDir.resolve("eashell_data.json");
        Path legacyFile = tempDir.resolve("legacy_unused.json");
        ScriptRepository repository = new ScriptRepository(dataFile, legacyFile);

        ScriptEntry original = new ScriptEntry("build", "C:\\a", List.of("npm install"));
        repository.add(original);

        // Mirrors what ScriptDialog does on edit: keep the same id, change the rest.
        ScriptEntry renamed = new ScriptEntry(original.getId(), "build-renamed", "C:\\a", List.of("npm install"));
        repository.update(original, renamed);

        List<ScriptEntry> afterUpdate = new ScriptRepository(dataFile, legacyFile).getAll();
        assertEquals(1, afterUpdate.size());
        assertEquals("build-renamed", afterUpdate.get(0).getName());
        assertEquals(original.getId(), afterUpdate.get(0).getId(), "renaming must preserve the stable id");

        repository.remove(renamed);

        assertTrue(new ScriptRepository(dataFile, legacyFile).getAll().isEmpty());
    }

    @Test
    void idsAreBackfilledAndPersistedForDataSavedBeforeIdsExisted(@TempDir Path tempDir) throws IOException {
        Path dataFile = tempDir.resolve("eashell_data.json");
        Path legacyFile = tempDir.resolve("legacy_unused.json");
        Files.writeString(dataFile,
                "[{\"name\":\"old-script\",\"workingDir\":\"C:\\\\old\",\"commands\":[\"echo hi\"]}]",
                StandardCharsets.UTF_8);

        ScriptRepository repository = new ScriptRepository(dataFile, legacyFile);
        String backfilledId = repository.getAll().get(0).getId();
        assertNotNull(backfilledId);

        // The backfilled id must be written back immediately, not re-generated on every
        // launch - otherwise a running script's id would drift out from under it.
        List<ScriptEntry> reloaded = new ScriptRepository(dataFile, legacyFile).getAll();
        assertEquals(backfilledId, reloaded.get(0).getId());
    }

    @Test
    void missingDataFileStartsEmptyWithoutError(@TempDir Path tempDir) {
        Path dataFile = tempDir.resolve("does-not-exist.json");
        Path legacyFile = tempDir.resolve("legacy_unused.json");

        ScriptRepository repository = new ScriptRepository(dataFile, legacyFile);

        assertTrue(repository.getAll().isEmpty());
        assertFalse(repository.wasDataFileCorrupt());
    }

    @Test
    void corruptDataFileIsBackedUpAndResetToEmpty(@TempDir Path tempDir) throws IOException {
        Path dataFile = tempDir.resolve("eashell_data.json");
        Path legacyFile = tempDir.resolve("legacy_unused.json");
        Files.writeString(dataFile, "{ this is not valid json", StandardCharsets.UTF_8);

        ScriptRepository repository = new ScriptRepository(dataFile, legacyFile);

        assertTrue(repository.wasDataFileCorrupt());
        assertTrue(repository.getAll().isEmpty());

        String backupPath = repository.getCorruptDataFileBackupPath();
        assertNotNull(backupPath);
        assertTrue(Files.exists(Path.of(backupPath)));
        assertFalse(Files.exists(dataFile));
    }

    @Test
    void legacyDataFileIsMigratedOnFirstRun(@TempDir Path tempDir) throws IOException {
        Path dataFile = tempDir.resolve("new-location").resolve("eashell_data.json");
        Path legacyFile = tempDir.resolve("eashell_data.json");
        Files.writeString(legacyFile,
                "[{\"name\":\"legacy-script\",\"workingDir\":\"C:\\\\old\",\"commands\":[\"echo hi\"]}]",
                StandardCharsets.UTF_8);

        ScriptRepository repository = new ScriptRepository(dataFile, legacyFile);

        assertTrue(Files.exists(dataFile));
        assertTrue(Files.exists(legacyFile), "legacy file should be copied, not moved");

        List<ScriptEntry> entries = repository.getAll();
        assertEquals(1, entries.size());
        assertEquals("legacy-script", entries.get(0).getName());
    }
}
