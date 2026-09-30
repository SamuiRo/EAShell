package com.eashell.util;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decides where EAShell keeps its data (eashell_data.json + the single-instance lock).
 *
 * Resolution order:
 *   1. -Deashell.data.dir=<path> ............ explicit override, wins over everything
 *   2. <app dir>/portable.txt exists ........ portable mode: <app dir>/data
 *                                             (skipped if <app dir> isn't writable, e.g. an
 *                                             image copied under C:\Program Files)
 *   3. otherwise ............................ per-user: %USERPROFILE%\.eashell
 *
 * "App dir" is the folder holding EAShell.exe for a jpackage build (the launcher sets
 * jpackage.app-path), or the folder holding the JAR for a plain java -jar run.
 */
public final class AppPaths {
    public static final String DATA_DIR_PROPERTY = "eashell.data.dir";
    public static final String PORTABLE_MARKER = "portable.txt";
    public static final String PORTABLE_DATA_DIR_NAME = "data";

    private static final Resolved RESOLVED = resolve(
            System.getProperty(DATA_DIR_PROPERTY),
            findAppDir(),
            Path.of(System.getProperty("user.home")));

    public static Path dataDir() {
        return RESOLVED.dataDir;
    }

    public static boolean isPortable() {
        return RESOLVED.portable;
    }

    public record Resolved(Path dataDir, boolean portable) {}

    // Pure function of its inputs so the decision can be unit-tested without touching
    // system properties or the real install location.
    static Resolved resolve(String override, Path appDir, Path userHome) {
        if (override != null && !override.isBlank()) {
            return new Resolved(Path.of(override).toAbsolutePath(), false);
        }
        if (appDir != null
                && Files.isRegularFile(appDir.resolve(PORTABLE_MARKER))
                && Files.isWritable(appDir)) {
            return new Resolved(appDir.resolve(PORTABLE_DATA_DIR_NAME), true);
        }
        return new Resolved(userHome.resolve(".eashell"), false);
    }

    private static Path findAppDir() {
        String launcher = System.getProperty("jpackage.app-path");
        if (launcher != null && !launcher.isBlank()) {
            return Path.of(launcher).toAbsolutePath().getParent();
        }
        try {
            Path codeSource = Path.of(AppPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            // A JAR's folder; for an exploded classes/ dir (mvn javafx:run) there is no
            // meaningful app dir, and the marker simply won't be found there.
            return Files.isRegularFile(codeSource) ? codeSource.getParent() : null;
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            return null;
        }
    }

    private AppPaths() {}
}
