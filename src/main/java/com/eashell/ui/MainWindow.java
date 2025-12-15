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
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

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

    // Map of active processes: script_name -> ProcessRunner
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

        // Create thread pool for running scripts
        this.executorService = Executors.newCachedThreadPool(r -> {
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

        // === MAIN CONTAINER (BorderPane) ===
        // Allows positioning elements: top, center, bottom, left, right
        BorderPane root = new BorderPane();
        root.setStyle(StyleManager.getRootStyle()); // Dark gradient background

        // === TOP PANEL ===
        TopBar topBar = new TopBar(
                this::handleAddScript,      // Callback for "+ NEW SCRIPT" button
                this::handleStopAll,        // Callback for "⏹ STOP ALL" button
                runningProcesses::size      // Function to get number of processes
        );
        root.setTop(topBar); // Position at top

        // === SPLIT PANEL (SplitPane) ===
        // Allows resizing left/right sections by dragging the divider
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle(StyleManager.getSplitPaneStyle());

        // === LEFT PANEL - SCRIPT LIST ===
        scriptListPanel = new ScriptListPanel(
                this::handleRunScript,      // Callback when "▶ RUN" is clicked
                this::handleEditScript,     // Callback when "✎ EDIT" is clicked
                this::handleDeleteScript    // Callback when "✖ DELETE" is clicked
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
        scene.getStylesheets().add(StyleManager.getStylesheet()); // CSS styles
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
        ScriptDialog.showAddDialog().ifPresent(entry -> {
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
        ScriptDialog.showEditDialog(entry).ifPresent(newEntry -> {
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
     * 4. Start ProcessRunner in a separate thread
     * 5. Update status on card (⚫ -> 🟢)
     */
    private void handleRunScript(ScriptEntry entry) {
        // Check if script is already running
        if (runningProcesses.containsKey(entry.getName())) {
            DeleteConfirmDialog.showAlreadyRunning(); // Show warning
            return;
        }

        // === STEP 1: CREATE RUNNER ===
        // Initially create ProcessRunner without outputArea and tab
        ProcessRunner runner = new ProcessRunner(entry, null, null);

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
        runningProcesses.put(entry.getName(), runner);

        // === STEP 6: UPDATE STATUS ON CARD ===
        updateScriptStatus(entry.getName(), true); // ⚫ -> 🟢

        // === STEP 7: START IN SEPARATE THREAD ===
        // ProcessRunner implements Runnable, so can be passed to executorService
        executorService.submit(runner);
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
        scriptListPanel.getStatusLabels().forEach((name, label) ->
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
     * @param scriptName - script name
     * @param running - whether script is running
     */
    private void updateScriptStatus(String scriptName, boolean running) {
        // Update UI in JavaFX main thread
        Platform.runLater(() -> {
            // Update status indicator on card (⚫/🟢)
            scriptListPanel.updateScriptStatus(scriptName, running);

            // If script stopped - remove from active processes map
            if (!running) {
                runningProcesses.remove(scriptName);
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