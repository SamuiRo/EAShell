package com.eashell.model;

import com.eashell.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ScriptRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<ScriptEntry> entries;

    public ScriptRepository() {
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

    private void save() {
        try (FileWriter writer = new FileWriter(Constants.DATA_FILE)) {
            GSON.toJson(entries, writer);
        } catch (IOException e) {
            System.err.println("Error saving entries: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadEntries() {
        try {
            if (Files.exists(Paths.get(Constants.DATA_FILE))) {
                String json = new String(Files.readAllBytes(Paths.get(Constants.DATA_FILE)));
                List<ScriptEntry> loaded = GSON.fromJson(json, new TypeToken<List<ScriptEntry>>(){}.getType());
                if (loaded != null) {
                    entries.clear();
                    entries.addAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading entries: " + e.getMessage());
            e.printStackTrace();
        }
    }
}