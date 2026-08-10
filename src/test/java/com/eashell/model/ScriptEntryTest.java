package com.eashell.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScriptEntryTest {

    @Test
    void equalityIsBasedOnIdOnly() {
        ScriptEntry a = new ScriptEntry("same-id", "build", "C:\\a", List.of("npm install"));
        ScriptEntry b = new ScriptEntry("same-id", "renamed", "C:\\different", List.of("npm test"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void entriesWithDifferentIdsAreNotEqualEvenWithTheSameName() {
        // The 3-arg constructor mints a fresh id every time - two "build" entries created
        // independently must not collide, unlike the old name-based equality.
        ScriptEntry a = new ScriptEntry("build", "C:\\a", List.of("npm install"));
        ScriptEntry b = new ScriptEntry("build", "C:\\a", List.of("npm install"));

        assertNotEquals(a, b);
    }

    @Test
    void constructorGeneratesANonNullId() {
        ScriptEntry entry = new ScriptEntry("build", "C:\\a", List.of("npm install"));
        assertNotNull(entry.getId());
    }

    @Test
    void roundTripsThroughGson() {
        Gson gson = new Gson();
        ScriptEntry original = new ScriptEntry("build", "C:\\project", List.of("npm install", "npm start"));

        ScriptEntry restored = gson.fromJson(gson.toJson(original), ScriptEntry.class);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getWorkingDir(), restored.getWorkingDir());
        assertEquals(original.getCommands(), restored.getCommands());
    }

    @Test
    void missingFieldsDeserializeToNull() {
        // ScriptRepository relies on this to detect and backfill entries saved before ids
        // existed, and the group field (ROADMAP.md §1) relies on the same behavior for
        // scripts saved before groups existed.
        Gson gson = new Gson();
        ScriptEntry restored = gson.fromJson("{\"name\":\"build\"}", ScriptEntry.class);

        assertEquals("build", restored.getName());
        assertNull(restored.getId());
        assertNull(restored.getWorkingDir());
        assertNull(restored.getCommands());
    }

    @Test
    void groupDefaultsToNullAndIsMutable() {
        ScriptEntry entry = new ScriptEntry("build", "C:\\a", List.of("npm install"));
        assertNull(entry.getGroup());

        entry.setGroup("backend");
        assertEquals("backend", entry.getGroup());
    }

    @Test
    void nullGroupRoundTripsThroughGsonAsNull() {
        Gson gson = new Gson();
        ScriptEntry entry = new ScriptEntry("build", "C:\\a", List.of("npm install"));

        ScriptEntry restored = gson.fromJson(gson.toJson(entry), ScriptEntry.class);

        assertNull(restored.getGroup());
    }
}
