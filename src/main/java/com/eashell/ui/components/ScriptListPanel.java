package com.eashell.ui.components;

import com.eashell.model.ScriptEntry;
import com.eashell.util.Constants;
import com.eashell.util.StyleManager;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    // Map for quick access to cards by the script's stable id (ScriptEntry.getId())
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
        getStyleClass().add("side-panel");

        // === HEADER "📋 SCRIPTS" ===
        Label header = new Label(Constants.SCRIPTS_HEADER); // "📋 SCRIPTS"
        header.getStyleClass().add("panel-header"); // Purple glowing text

        // === SCROLLABLE AREA ===
        // Allows scrolling the list if there are many scripts. Transparent background comes
        // from the .scroll-pane rule in app.css - ScrollPane carries that class by default.
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true); // Cards stretch to full width

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
     * Clears old cards and creates new ones for each script, grouped into a collapsible
     * section per distinct ScriptEntry.getGroup() (null -> "Ungrouped"). Grouping is a
     * view-only concern - it doesn't touch the repository or how scripts run.
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

        Map<String, List<ScriptEntry>> byGroup = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getGroup() == null ? Constants.UNGROUPED_LABEL : e.getGroup(),
                        TreeMap::new, Collectors.toList()));

        byGroup.forEach((groupName, groupEntries) ->
                scriptListContainer.getChildren().add(createGroupPane(groupName, groupEntries)));
    }

    private TitledPane createGroupPane(String groupName, List<ScriptEntry> groupEntries) {
        VBox cardsBox = new VBox(10);
        cardsBox.setPadding(new Insets(5, 0, 0, 0));

        for (ScriptEntry entry : groupEntries) {
            ScriptCard card = new ScriptCard(entry, onRun, onEdit, onDelete);

            // Save card in map for quick access, keyed by the stable id (not the display
            // name, which is neither unique nor stable across edits)
            scriptCards.put(entry.getId(), card);

            cardsBox.getChildren().add(card);
        }

        TitledPane pane = new TitledPane("", cardsBox);
        pane.setGraphic(createGroupHeader(groupName, groupEntries));
        pane.setExpanded(true);
        return pane;
    }

    /**
     * Group header: name + a button that runs every script in the group. No new execution
     * logic - it just calls the same onRun callback used by each card's own RUN button.
     */
    private HBox createGroupHeader(String groupName, List<ScriptEntry> groupEntries) {
        Label nameLabel = new Label(groupName);
        nameLabel.getStyleClass().add("group-header-label");

        Button runGroupBtn = StyleManager.createSmallButton(Constants.GROUP_RUN_BUTTON, StyleManager.BTN_ACCENT);
        runGroupBtn.setOnAction(e -> groupEntries.forEach(onRun));
        // Without this, clicking the button also toggles the TitledPane's expand/collapse.
        runGroupBtn.setOnMouseClicked(Event::consume);

        HBox headerBox = new HBox(10, nameLabel, runGroupBtn);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        return headerBox;
    }

    /**
     * MESSAGE WHEN NO SCRIPTS
     *
     * Displayed when user hasn't added any scripts yet.
     * Hints to click "NEW SCRIPT" to get started.
     */
    private void showEmptyMessage() {
        Label emptyLabel = new Label("No scripts added yet.\nClick 'NEW SCRIPT' to get started.");
        emptyLabel.getStyleClass().add("empty-list-label"); // Gray centered text
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
     * @param scriptId - stable script id
     * @param running - whether script is running
     */
    public void updateScriptStatus(String scriptId, boolean running) {
        // Find card by script id
        ScriptCard card = scriptCards.get(scriptId);

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
     * Returns map: script_id -> status_indicator
     * Used in handleStopAll() for bulk status updates.
     *
     * @return map with status indicators of all scripts
     */
    public Map<String, Label> getStatusLabels() {
        Map<String, Label> labels = new HashMap<>();

        // Extract status indicator from each card
        scriptCards.forEach((id, card) -> labels.put(id, card.getStatusLabel()));

        return labels;
    }
}