package com.eashell.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScriptEntryTest {

    @Test
    void equalityIsBasedOnNameOnly() {
        ScriptEntry a = new ScriptEntry("build", "C:\\a", List.of("npm install"));
        ScriptEntry b = new ScriptEntry("build", "C:\\different", List.of("npm test"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void entriesWithDifferentNamesAreNotEqual() {
        ScriptEntry a = new ScriptEntry("build", "C:\\a", List.of("npm install"));
        ScriptEntry b = new ScriptEntry("deploy", "C:\\a", List.of("npm install"));

        assertNotEquals(a, b);
    }

    @Test
    void roundTripsThroughGson() {
        Gson gson = new Gson();
        ScriptEntry original = new ScriptEntry("build", "C:\\project", List.of("npm install", "npm start"));

        ScriptEntry restored = gson.fromJson(gson.toJson(original), ScriptEntry.class);

        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getWorkingDir(), restored.getWorkingDir());
        assertEquals(original.getCommands(), restored.getCommands());
    }

    @Test
    void missingFieldsDeserializeToNull() {
        // ROADMAP.md §1 relies on this: an additive field like `group` must default to null
        // for existing data files without a migration step.
        Gson gson = new Gson();
        ScriptEntry restored = gson.fromJson("{\"name\":\"build\"}", ScriptEntry.class);

        assertEquals("build", restored.getName());
        assertNull(restored.getWorkingDir());
        assertNull(restored.getCommands());
    }
}
