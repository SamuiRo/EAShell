package com.eashell.model;

import com.eashell.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScriptRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataFile;
    private final Path legacyDataFile;
    private final List<ScriptEntry> entries;
    private boolean dataFileWasCorrupt = false;
    private String corruptDataFileBackupPath;

    public ScriptRepository() {
        this(Constants.DATA_FILE, Constants.LEGACY_DATA_FILE);
    }

    // Package-private: lets tests point the repository at a @TempDir instead of the real
    // per-user data file.
    ScriptRepository(Path dataFile, Path legacyDataFile) {
        this.dataFile = dataFile;
        this.legacyDataFile = legacyDataFile;
        this.entries = new ArrayList<>();
        loadEntries();
    }

    public List<ScriptEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public void add(ScriptEntry entry) {
        entries.add(entry);
        save();
    }

    public void update(ScriptEntry oldEntry, ScriptEntry newEntry) {
        int index = entries.indexOf(oldEntry);
        if (index >= 0) {
            entries.set(index, newEntry);
            save();
        }
    }

    public void remove(ScriptEntry entry) {
        entries.remove(entry);
        save();
    }

    public ScriptEntry findByName(String name) {
        return entries.stream()
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether the data file was found corrupted (invalid JSON) on load and reset to empty.
     * The UI layer is responsible for telling the user - the repository stays UI-agnostic.
     */
    public boolean wasDataFileCorrupt() {
        return dataFileWasCorrupt;
    }

    public String getCorruptDataFileBackupPath() {
        return corruptDataFileBackupPath;
    }

    private void save() {
        try {
            Files.createDirectories(dataFile.getParent());

            // Write to a temp file and rename over the real one, so a crash or a full disk
            // mid-write can never leave a truncated, unparseable eashell_data.json behind.
            Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(entries, writer);
            }
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Error saving entries: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadEntries() {
        try {
            migrateLegacyDataFileIfNeeded();

            if (Files.exists(dataFile)) {
                String json = Files.readString(dataFile, StandardCharsets.UTF_8);
                List<ScriptEntry> loaded = GSON.fromJson(json, new TypeToken<List<ScriptEntry>>(){}.getType());
                if (loaded != null) {
                    entries.clear();
                    entries.addAll(loaded);
                    backfillMissingIds();
                }
            }
        } catch (JsonSyntaxException e) {
            recoverFromCorruptDataFile();
        } catch (IOException e) {
            System.err.println("Error loading entries: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Data files saved before ids existed deserialize with a null id. Assign a stable one
     * and persist it immediately so identity doesn't keep changing across relaunches -
     * runningProcesses/scriptCards are keyed by id, so a stable id is what lets renaming a
     * running script keep working.
     */
    private void backfillMissingIds() {
        boolean anyMissing = false;
        for (ScriptEntry entry : entries) {
            if (entry.getId() == null) {
                entry.setId(UUID.randomUUID().toString());
                anyMissing = true;
            }
        }
        if (anyMissing) {
            save();
        }
    }

    /**
     * One-time migration for existing installs: the data file used to live next to the
     * executable (CWD-relative), which silently lost scripts if launched from elsewhere.
     * Copy - not move - so the old file stays as a backup if anything goes wrong.
     */
    private void migrateLegacyDataFileIfNeeded() throws IOException {
        if (!Files.exists(dataFile) && Files.exists(legacyDataFile)) {
            Files.createDirectories(dataFile.getParent());
            Files.copy(legacyDataFile, dataFile);
        }
    }

    private void recoverFromCorruptDataFile() {
        dataFileWasCorrupt = true;
        try {
            Path backup = dataFile.resolveSibling(
                    dataFile.getFileName() + ".corrupt-" + System.currentTimeMillis());
            Files.move(dataFile, backup, StandardCopyOption.REPLACE_EXISTING);
            corruptDataFileBackupPath = backup.toString();
        } catch (IOException moveError) {
            System.err.println("Could not rename corrupt data file: " + moveError.getMessage());
            corruptDataFileBackupPath = dataFile.toString();
        }
        entries.clear();
    }
}
