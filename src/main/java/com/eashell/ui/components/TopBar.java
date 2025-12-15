package com.eashell.ui.components;

import com.eashell.util.Constants;
import com.eashell.util.StyleManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

/**
 * TOP PANEL (HEADER)
 *
 * Displayed at the very top of the application window.
 * Contains application name, running script counter, and action buttons.
 *
 * Visual structure:
 * ┌──────────────────────────────────────────────────────────────┐
 * │ ⚡ Shell   Running: 2   [space]   [+ NEW SCRIPT] [⏹ STOP ALL] │
 * └──────────────────────────────────────────────────────────────┘
 *   ^          ^             ^          ^              ^
 *   name       counter       spacer     add button     stop button
 */
public class TopBar extends HBox {
    // Running script counter indicator ("Running: 0", "Running: 2", etc.)
    private final Label statusLabel;

    // Button callbacks
    private final Runnable onAddScript;  // Called when "+ NEW SCRIPT" is clicked
    private final Runnable onStopAll;    // Called when "⏹ STOP ALL" is clicked

    // Function that returns number of running processes
    private final Supplier<Integer> runningCountSupplier;

    public TopBar(Runnable onAddScript, Runnable onStopAll, Supplier<Integer> runningCountSupplier) {
        this.onAddScript = onAddScript;
        this.onStopAll = onStopAll;
        this.runningCountSupplier = runningCountSupplier;
        this.statusLabel = new Label("Running: 0");

        initialize();
        startStatusUpdater(); // Start timer for counter updates
    }

    /**
     * INITIALIZE VISUAL ELEMENTS
     */
    private void initialize() {
        // Spacing between elements
        setSpacing(20);
        setPadding(new Insets(15, 20, 15, 20));
        setAlignment(Pos.CENTER_LEFT);

        // Dark gradient background with purple bottom border
        setStyle(StyleManager.getTopBarStyle());

        // === APPLICATION NAME "⚡ Shell" ===
        Label title = new Label(Constants.TITLE_LABEL); // "⚡ Shell"
        title.setStyle(StyleManager.getTitleStyle()); // Large text with gradient and glow

        // === RUNNING SCRIPT COUNTER ===
        statusLabel.setId("global-status");
        statusLabel.setStyle(StyleManager.getStatusLabelStyle()); // Purple text
        // Text updates automatically every second via startStatusUpdater()

        // === SPACER (EXPANDABLE SPACE) ===
        // Takes all available width, pushing buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // === ADD NEW SCRIPT BUTTON ===
        Button addButton = StyleManager.createStyledButton("+ NEW SCRIPT", StyleManager.PRIMARY_BUTTON);
        addButton.setOnAction(e -> onAddScript.run()); // Opens script creation dialog

        // === STOP ALL SCRIPTS BUTTON ===
        Button stopAllButton = StyleManager.createStyledButton("⏹ STOP ALL", StyleManager.DANGER_BUTTON);
        stopAllButton.setOnAction(e -> onStopAll.run()); // Stops all active processes

        // Add all elements in horizontal order
        getChildren().addAll(title, statusLabel, spacer, addButton, stopAllButton);
    }

    /**
     * AUTOMATIC COUNTER UPDATE
     *
     * Creates a background timer that checks every second
     * the number of running processes and updates the "Running: X" text.
     *
     * This is needed because scripts can finish on their own (without clicking STOP),
     * and then the counter will automatically decrease.
     */
    private void startStatusUpdater() {
        Timer timer = new Timer(true); // true = daemon thread (terminates with application)

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Get current number of running processes
                int running = runningCountSupplier.get();

                // Update UI in JavaFX main thread
                Platform.runLater(() -> statusLabel.setText("Running: " + running));
            }
        }, 0, 1000); // Start immediately, repeat every 1000ms (1 second)
    }
}