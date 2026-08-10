package com.eashell.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Constants {
    // Files
    public static final String DATA_FILE = "eashell_data.json";

    // Buffer settings
    public static final int MAX_BUFFER_SIZE = 10000;
    public static final int UI_UPDATE_INTERVAL_MS = 100;
    public static final int READER_BUFFER_SIZE = 8192;
    public static final int FLUSH_THRESHOLD = 4096;

    // Process settings
    public static final int PROCESS_STOP_TIMEOUT_SECONDS = 2;
    public static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;
    public static final Charset CONSOLE_CHARSET = StandardCharsets.UTF_8;

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

    // Emojis
    public static final String STATUS_RUNNING = "🟢";
    public static final String STATUS_STOPPED = "⚫";
    public static final String STATUS_SUCCESS = "✓";
    public static final String STATUS_ERROR = "✗";
    public static final String STATUS_TERMINATED = "⏹";

    private Constants() {} // Prevent instantiation
}