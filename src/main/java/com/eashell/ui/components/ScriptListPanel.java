package com.eashell.ui.components;

import com.eashell.model.ScriptEntry;
import com.eashell.util.Constants;
import com.eashell.util.StyleManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * SCRIPT LIST PANEL (LEFT SIDE OF WINDOW)
 *
 * Responsible for displaying all saved scripts as a vertical list of cards.
 * Each script is represented by a separate ScriptCard.
 *
 * Structure:
 * ┌──────────────────────────────┐
 * │ 📋 SCRIPTS      (header)     │
 * ├──────────────────────────────┤
 * │ ┌──────────────────────────┐ │
 * │ │ [ScriptCard 1]           │ │ <- Script card
 * │ │ [ScriptCard 2]           │ │
 * │ │ [ScriptCard 3]           │ │ <- Scrollable list
 * │ │ ...                      │ │
 * │ └──────────────────────────┘ │
 * └──────────────────────────────┘
 */
public class ScriptListPanel extends VBox {
    // Container that holds all script cards
    private final VBox scriptListContainer;

    // Map for quick access to cards by script name
    // Used to update status (⚫/🟢) when script starts
    private final Map<String, ScriptCard> scriptCards;

    // Callbacks for handling user actions
    private final Consumer<ScriptEntry> onRun;    // Run script
    private final Consumer<ScriptEntry> onEdit;   // Edit script
    private final Consumer<ScriptEntry> onDelete; // Delete script

    public ScriptListPanel(Consumer<ScriptEntry> onRun,
                           Consumer<ScriptEntry> onEdit,
                           Consumer<ScriptEntry> onDelete) {
        this.scriptCards = new HashMap<>();
        this.onRun = onRun;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        // Spacing between panel elements
        setSpacing(10);
        setPadding(new Insets(20));

        // Dark gradient panel background
        setStyle(StyleManager.getPanelStyle());

        // === HEADER "📋 SCRIPTS" ===
        Label header = new Label(Constants.SCRIPTS_HEADER); // "📋 SCRIPTS"
        header.setStyle(StyleManager.getHeaderStyle()); // Purple glowing text

        // === SCROLLABLE AREA ===
        // Allows scrolling the list if there are many scripts
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true); // Cards stretch to full width
        scrollPane.setStyle(StyleManager.getScrollPaneStyle()); // Transparent background

        // === CARD CONTAINER ===
        scriptListContainer = new VBox(10); // 10px between cards
        scriptListContainer.setPadding(new Insets(10));
        scrollPane.setContent(scriptListContainer);

        // JavaFX derives the wheel step from content height, so it shrinks as the list
        // grows. Scale it back up to a fixed, comfortable speed.
        scriptListContainer.setOnScroll(e -> {
            double delta = e.getDeltaY() * Constants.SCROLL_SPEED_FACTOR;
            scrollPane.setVvalue(scrollPane.getVvalue() - delta / scriptListContainer.getHeight());
        });

        // ScrollPane stretches to full available height
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Add all elements: header, separator, scrollable area
        getChildren().addAll(header, new Separator(), scrollPane);
    }

    /**
     * UPDATE SCRIPT LIST
     *
     * Called when:
     * - New script is added (+ NEW SCRIPT)
     * - Existing script is edited (✎ EDIT)
     * - Script is deleted (✖ DELETE)
     * - On application startup (loading from JSON file)
     *
     * Clears old cards and creates new ones for each script.
     */
    public void refresh(List<ScriptEntry> entries) {
        // Remove all old cards
        scriptListContainer.getChildren().clear();
        scriptCards.clear();

        // If no scripts - show hint
        if (entries.isEmpty()) {
            showEmptyMessage();
            return;
        }

        // Create card for each script
        for (ScriptEntry entry : entries) {
            ScriptCard card = new ScriptCard(entry, onRun, onEdit, onDelete);

            // Save card in map for quick access
            scriptCards.put(entry.getName(), card);

            // Add card to container
            scriptListContainer.getChildren().add(card);
        }
    }

    /**
     * MESSAGE WHEN NO SCRIPTS
     *
     * Displayed when user hasn't added any scripts yet.
     * Hints to click "NEW SCRIPT" to get started.
     */
    private void showEmptyMessage() {
        Label emptyLabel = new Label("No scripts added yet.\nClick 'NEW SCRIPT' to get started.");
        emptyLabel.setStyle(StyleManager.getEmptyLabelStyle()); // Gray centered text
        emptyLabel.setAlignment(Pos.CENTER);
        scriptListContainer.getChildren().add(emptyLabel);
    }

    /**
     * UPDATE SCRIPT STATUS
     *
     * Changes status indicator (⚫/🟢) on card when:
     * - Script starts (running = true) -> 🟢 green with glow
     * - Script stops (running = false) -> ⚫ gray
     *
     * @param scriptName - script name
     * @param running - whether script is running
     */
    public void updateScriptStatus(String scriptName, boolean running) {
        // Find card by script name
        ScriptCard card = scriptCards.get(scriptName);

        if (card != null) {
            if (running) {
                // Set status to "running" (🟢)
                StyleManager.setRunningStatus(card.getStatusLabel());
            } else {
                // Set status to "stopped" (⚫)
                StyleManager.setStoppedStatus(card.getStatusLabel());
            }
        }
    }

    /**
     * GET ALL STATUS INDICATORS
     *
     * Returns map: script_name -> status_indicator
     * Used in handleStopAll() for bulk status updates.
     *
     * @return map with status indicators of all scripts
     */
    public Map<String, Label> getStatusLabels() {
        Map<String, Label> labels = new HashMap<>();

        // Extract status indicator from each card
        scriptCards.forEach((name, card) -> labels.put(name, card.getStatusLabel()));

        return labels;
    }
}