package com.eashell.ui;

import com.eashell.model.ScriptEntry;
import com.eashell.model.ScriptRepository;
import com.eashell.service.ProcessRunner;
import com.eashell.ui.components.OutputPanel;
import com.eashell.ui.components.ScriptListPanel;
import com.eashell.ui.components.TopBar;
import com.eashell.ui.dialogs.DeleteConfirmDialog;
import com.eashell.ui.dialogs.ScriptDialog;
import com.eashell.util.Constants;
import com.eashell.util.StyleManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * MAIN APPLICATION WINDOW
 *
 * Responsible for creating and managing the entire UI of the application.
 * Combines all components and handles all user actions.
 *
 * Window structure:
 * ┌──────────────────────────────────────────────────────────┐
 * │ TopBar: ⚡ Shell  Running: 2      [+ NEW] [⏹ STOP ALL]    │ <- Top panel
 * ├────────────────────────┬─────────────────────────────────┤
 * │ ScriptListPanel        │ OutputPanel                     │
 * │ ┌────────────────────┐ │ ┌─────────────────────────────┐ │
 * │ │ [ScriptCard 1]     │ │ │ [Tab1] [Tab2]               │ │
 * │ │ [ScriptCard 2]     │ │ │ ┌─────────────────────────┐ │ │
 * │ │ [ScriptCard 3]     │ │ │ │ Console output          │ │ │
 * │ │ ...                │ │ │ │ ...                     │ │ │
 * │ └────────────────────┘ │ │ └─────────────────────────┘ │ │
 * │                        │ │ [⏹ STOP] [🗑 CLEAR]         │ │
 * │                        │ └─────────────────────────────┘ │
 * └────────────────────────┴─────────────────────────────────┘
 *         40%       |               60%
 *         Left      |               Right
 *         panel     |               panel
 */
public class MainWindow {
    // Main JavaFX window
    private final Stage primaryStage;

    // Repository for saving/loading scripts from JSON file
    private final ScriptRepository repository;

    // Map of active or queued processes: script_id -> ProcessRunner (keyed by ScriptEntry.getId(),
    // NOT the display name - see CLAUDE.md rule 2).
    // ConcurrentHashMap because there can be concurrent access from different threads
    private final Map<String, ProcessRunner> runningProcesses;

    // Thread pool for executing scripts in the background
    private final ExecutorService executorService;

    // UI components
    private ScriptListPanel scriptListPanel; // Left panel with script list
    private OutputPanel outputPanel;         // Right panel with output

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.repository = new ScriptRepository();
        this.runningProcesses = new ConcurrentHashMap<>();

        // Bounded so running a whole group of scripts can't spawn unlimited threads,
        // OS processes and console tabs at once - excess runs just queue.
        this.executorService = Executors.newFixedThreadPool(Constants.MAX_CONCURRENT_SCRIPTS, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // Daemon threads terminate when the application closes
            return t;
        });
    }

    /**
     * WINDOW DISPLAY
     *
     * Main method that creates the entire UI and shows the window.
     * Called from main() when starting the application.
     */
    public void show() {
        primaryStage.setTitle(Constants.APP_TITLE); // "EA Shell"

        // Data file was invalid JSON and got reset - tell the user before anything else,
        // since otherwise an empty script list with no explanation looks like data loss.
        if (repository.wasDataFileCorrupt()) {
            new Alert(Alert.AlertType.WARNING,
                    "Your saved script list was corrupted and could not be read, so it was reset "
                            + "to empty.\n\nThe broken file was saved as:\n"
                            + repository.getCorruptDataFileBackupPath())
                    .showAndWait();
        }

        // === MAIN CONTAINER (BorderPane) ===
        // Allows positioning elements: top, center, bottom, left, right
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root"); // Dark gradient background

        // === TOP PANEL ===
        TopBar topBar = new TopBar(
                this::handleAddScript,      // Callback for "+ NEW SCRIPT" button
                this::handleStopAll,        // Callback for "⏹ STOP ALL" button
                runningProcesses::size      // Function to get number of processes
        );
        root.setTop(topBar); // Position at top

        // === SPLIT PANEL (SplitPane) ===
        // Allows resizing left/right sections by dragging the divider
        // Styled via the .split-pane rule in app.css - SplitPane already carries that
        // style class by default, nothing to add here.
        SplitPane splitPane = new SplitPane();

        // === LEFT PANEL - SCRIPT LIST ===
        scriptListPanel = new ScriptListPanel(
                this::handleRunScript,      // Callback when "▶ RUN" is clicked
                this::handleEditScript,     // Callback when "✎ EDIT" is clicked
                this::handleDeleteScript,   // Callback when "✖ DELETE" is clicked
                this::handleRunGroup        // Callback when a group's "▶ RUN GROUP" is clicked
        );

        // === RIGHT PANEL - CONSOLE OUTPUT ===
        outputPanel = new OutputPanel();

        // Add panels to SplitPane
        splitPane.getItems().addAll(scriptListPanel, outputPanel);

        // Set initial divider position (40% left, 60% right)
        splitPane.setDividerPositions(Constants.SPLIT_PANE_DIVIDER_POSITION); // 0.4 = 40%

        root.setCenter(splitPane); // Position in center

        // === CREATE SCENE AND WINDOW ===
        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT); // 1400x800
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        primaryStage.setScene(scene);

        // Window close handler - stop all processes
        primaryStage.setOnCloseRequest(e -> cleanup());

        // Load scripts from JSON file
        refreshScriptList();

        // Show window
        primaryStage.show();
    }

    // =========================================================================
    // USER ACTION HANDLERS
    // =========================================================================

    /**
     * ADD NEW SCRIPT
     *
     * Called when the "+ NEW SCRIPT" button in TopBar is clicked.
     * Opens a dialog for entering new script data.
     */
    private void handleAddScript() {
        // Show dialog and get Optional<ScriptEntry>
        ScriptDialog.showAddDialog(repository.getAll()).ifPresent(entry -> {
            repository.add(entry);        // Save to JSON
            refreshScriptList();          // Update card list
        });
    }

    /**
     * EDIT SCRIPT
     *
     * Called when the "✎ EDIT" button on a script card is clicked.
     * Opens a dialog with pre-filled data.
     */
    private void handleEditScript(ScriptEntry entry) {
        // Show edit dialog with existing data
        ScriptDialog.showEditDialog(entry, repository.getAll()).ifPresent(newEntry -> {
            repository.update(entry, newEntry); // Update in JSON
            refreshScriptList();                // Update card list
        });
    }

    /**
     * DELETE SCRIPT
     *
     * Called when the "✖ DELETE" button on a script card is clicked.
     * Shows confirmation dialog before deletion.
     */
    private void handleDeleteScript(ScriptEntry entry) {
        // Show confirmation dialog
        if (DeleteConfirmDialog.confirm(entry)) {
            repository.remove(entry);    // Remove from JSON
            refreshScriptList();         // Update card list
        }
    }

    /**
     * RUN SCRIPT
     *
     * Called when the "▶ RUN" button on a script card is clicked.
     *
     * Algorithm:
     * 1. Check if script is not already running
     * 2. Create ProcessRunner
     * 3. Create tab in OutputPanel
     * 4. Submit ProcessRunner to the (bounded) executor
     * 5. Mark the card queued (🟡) - ProcessRunner.run() itself fires the 🟢 "actually
     *    running" update as its first action, once the pool has a free thread for it
     */
    private void handleRunScript(ScriptEntry entry) {
        // Check if script is already running (or queued)
        if (runningProcesses.containsKey(entry.getId())) {
            DeleteConfirmDialog.showAlreadyRunning(); // Show warning
            return;
        }

        // === STEP 1: CREATE RUNNER ===
        // Initially create ProcessRunner without outputArea and tab
        ProcessRunner runner = new ProcessRunner(entry, null, null, this::updateScriptStatus);

        // === STEP 2: CREATE TAB IN RIGHT PANEL ===
        // Pass runner to outputPanel for tab creation
        Tab outputTab = outputPanel.createOutputTab(
                entry,
                runner,  // Now passing the actual runner!
                this::updateScriptStatus // Callback for updating card status
        );

        // === STEP 3: GET TEXTAREA FROM TAB ===
        // Needed so ProcessRunner can write output to this area
        TextArea outputArea = outputPanel.getOutputAreaFromTab(outputTab);

        // === STEP 4: UPDATE RUNNER WITH CORRECT REFERENCES ===
        runner.setOutputArea(outputArea); // Set where to write output
        runner.setTab(outputTab);         // Set tab for updating title

        // === STEP 5: SAVE RUNNER IN MAP ===
        runningProcesses.put(entry.getId(), runner);

        // === STEP 6: MARK QUEUED ON CARD (⚫ -> 🟡) ===
        // Not "running" yet - the bounded pool (MAX_CONCURRENT_SCRIPTS) may not have a free
        // thread for it right away, and claiming 🟢 before a single command has actually
        // started would be misleading (and would inflate "Running: N" for work not yet begun).
        scriptListPanel.updateScriptQueuedStatus(entry.getId());

        // === STEP 7: SUBMIT TO EXECUTOR ===
        // ProcessRunner implements Runnable; excess submissions beyond the pool size queue here.
        executorService.submit(runner);
    }

    /**
     * RUN A WHOLE GROUP
     *
     * Called when a group's "▶ RUN GROUP" button is clicked. Silently skips scripts that are
     * already running (or queued) instead of firing handleRunScript's blocking "already
     * running" dialog once per hit - that warning is the right behavior for an explicit
     * single-card click, not for a bulk action over a whole group.
     */
    private void handleRunGroup(List<ScriptEntry> entries) {
        entries.stream()
                .filter(entry -> !runningProcesses.containsKey(entry.getId()))
                .forEach(this::handleRunScript);
    }

    /**
     * STOP ALL SCRIPTS
     *
     * Called when the "⏹ STOP ALL" button in TopBar is clicked.
     * Stops all active processes and updates statuses on cards.
     */
    private void handleStopAll() {
        // Stop all processes
        runningProcesses.values().forEach(ProcessRunner::stop);
        runningProcesses.clear(); // Clear map

        // Update statuses on all cards (🟢 -> ⚫)
        scriptListPanel.getStatusLabels().forEach((id, label) ->
                StyleManager.setStoppedStatus(label)
        );
    }

    /**
     * UPDATE SCRIPT STATUS ON CARD
     *
     * Called when:
     * - Script starts (running = true)
     * - Script finishes (running = false)
     * - User closes tab (running = false)
     *
     * @param scriptId - stable script id (ScriptEntry.getId()), not the display name
     * @param running - whether script is running
     */
    private void updateScriptStatus(String scriptId, boolean running) {
        // Update UI in JavaFX main thread
        Platform.runLater(() -> {
            // Update status indicator on card (⚫/🟢)
            scriptListPanel.updateScriptStatus(scriptId, running);

            // If script stopped - remove from active processes map
            if (!running) {
                runningProcesses.remove(scriptId);
            }
        });
    }

    /**
     * UPDATE SCRIPT CARD LIST
     *
     * Loads all scripts from JSON file and displays them as cards.
     * Called after adding/editing/deleting a script.
     */
    private void refreshScriptList() {
        scriptListPanel.refresh(repository.getAll());

        // refresh() rebuilds every card from scratch, so freshly built cards start
        // stopped even for scripts that are still running (or queued) - reapply the
        // authoritative state from runningProcesses. isRunning() is false for a runner that's
        // been submitted but not yet dequeued by the bounded pool (process is still null).
        runningProcesses.forEach((id, runner) -> {
            if (runner.isRunning()) {
                scriptListPanel.updateScriptStatus(id, true);
            } else {
                scriptListPanel.updateScriptQueuedStatus(id);
            }
        });
    }

    /**
     * CLEANUP RESOURCES ON APPLICATION CLOSE
     *
     * Called when user closes the window.
     * Stops all processes and shuts down thread pool.
     */
    private void cleanup() {
        // Stop all active scripts
        handleStopAll();

        // Shut down thread pool
        executorService.shutdownNow();

        try {
            // Wait maximum 5 seconds for all threads to terminate
            executorService.awaitTermination(
                    Constants.EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}