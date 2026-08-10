package com.eashell.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Constants {
    // Files
    public static final String DATA_FILE_NAME = "eashell_data.json";
    // Per-user location, independent of the launch directory - required for an installed app,
    // which cannot rely on being able to write next to its own executable.
    public static final Path DATA_DIR = Path.of(System.getProperty("user.home"), ".eashell");
    public static final Path DATA_FILE = DATA_DIR.resolve(DATA_FILE_NAME);
    // Old CWD-relative location, kept only to migrate existing users' data on first run.
    public static final Path LEGACY_DATA_FILE = Path.of(DATA_FILE_NAME);
    public static final Path LOCK_FILE = DATA_DIR.resolve("eashell.lock");

    // Buffer settings
    public static final int MAX_BUFFER_SIZE = 10000;
    public static final int UI_UPDATE_INTERVAL_MS = 100;
    public static final int READER_BUFFER_SIZE = 8192;
    public static final int FLUSH_THRESHOLD = 4096;

    // Process settings
    public static final int PROCESS_STOP_TIMEOUT_SECONDS = 2;
    public static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;
    public static final Charset CONSOLE_CHARSET = StandardCharsets.UTF_8;
    // Bounded so running a large group can't spawn unlimited threads/processes/tabs at once.
    public static final int MAX_CONCURRENT_SCRIPTS = 8;

    // UI dimensions
    public static final int WINDOW_WIDTH = 1400;
    public static final int WINDOW_HEIGHT = 800;
    public static final double SPLIT_PANE_DIVIDER_POSITION = 0.4;
    public static final double SCROLL_SPEED_FACTOR = 4;

    // UI texts
    public static final String APP_TITLE = "EA Shell";
    public static final String TITLE_LABEL = "⚡ Shell";
    public static final String SCRIPTS_HEADER = "📋 SCRIPTS";
    public static final String OUTPUT_HEADER = "📟 CONSOLE";
    public static final String STDIN_PROMPT = "Type input and press Enter to send it to the running process...";
    public static final String UNGROUPED_LABEL = "Ungrouped";
    public static final String GROUP_RUN_BUTTON = "▶ RUN GROUP";
    public static final String GROUP_FIELD_PROMPT = "Ungrouped";

    // Emojis
    public static final String STATUS_RUNNING = "🟢";
    public static final String STATUS_QUEUED = "🟡";
    public static final String STATUS_STOPPED = "⚫";
    public static final String STATUS_SUCCESS = "✓";
    public static final String STATUS_ERROR = "✗";
    public static final String STATUS_TERMINATED = "⏹";

    private Constants() {} // Prevent instantiation
}