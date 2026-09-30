package com.eashell.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPathsTest {

    @TempDir
    Path tmp;

    @Test
    void defaultsToUserHomeWithoutMarker() throws IOException {
        Path appDir = Files.createDirectories(tmp.resolve("app"));
        Path home = tmp.resolve("home");

        AppPaths.Resolved r = AppPaths.resolve(null, appDir, home);

        assertEquals(home.resolve(".eashell"), r.dataDir());
        assertFalse(r.portable());
    }

    @Test
    void markerNextToAppSwitchesToPortableDataDir() throws IOException {
        Path appDir = Files.createDirectories(tmp.resolve("app"));
        Files.createFile(appDir.resolve(AppPaths.PORTABLE_MARKER));

        AppPaths.Resolved r = AppPaths.resolve(null, appDir, tmp.resolve("home"));

        assertEquals(appDir.resolve(AppPaths.PORTABLE_DATA_DIR_NAME), r.dataDir());
        assertTrue(r.portable());
    }

    @Test
    void explicitOverrideWinsOverMarker() throws IOException {
        Path appDir = Files.createDirectories(tmp.resolve("app"));
        Files.createFile(appDir.resolve(AppPaths.PORTABLE_MARKER));
        Path custom = tmp.resolve("custom");

        AppPaths.Resolved r = AppPaths.resolve(custom.toString(), appDir, tmp.resolve("home"));

        assertEquals(custom.toAbsolutePath(), r.dataDir());
        assertFalse(r.portable());
    }

    @Test
    void unknownAppDirFallsBackToUserHome() {
        Path home = tmp.resolve("home");

        AppPaths.Resolved r = AppPaths.resolve("  ", null, home);

        assertEquals(home.resolve(".eashell"), r.dataDir());
        assertFalse(r.portable());
    }
}
