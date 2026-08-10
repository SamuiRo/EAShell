package com.eashell.ui.components;

import com.eashell.model.ScriptEntry;
import com.eashell.util.StyleManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.CacheHint;

import java.util.function.Consumer;

/**
 * SCRIPT CARD (INDIVIDUAL LIST ITEM)
 *
 * Visually represents one script in the left panel.
 * Each card contains name, path, commands, and control buttons.
 *
 * Visual card structure:
 * ┌────────────────────────────────────────┐
 * │ Script Name ⚫        (name + status)  │
 * │ 📁 /path/to/folder   (working dir)    │
 * │ ▶ npm install        (command 1)      │
 * │ ▶ npm start          (command 2)      │
 * │                                        │
 * │ [▶ RUN] [✎ EDIT] [✖ DELETE] (buttons) │
 * └────────────────────────────────────────┘
 *
 * Statuses:
 * ⚫ - stopped (gray)
 * 🟢 - running (green/purple with glow)
 */
public class ScriptCard extends VBox {
    private final ScriptEntry entry;           // Script data
    private final Label statusLabel;           // Status indicator (⚫/🟢)
    private final Consumer<ScriptEntry> onRun; // Callback when RUN is clicked
    private final Consumer<ScriptEntry> onEdit; // Callback when EDIT is clicked
    private final Consumer<ScriptEntry> onDelete; // Callback when DELETE is clicked

    public ScriptCard(ScriptEntry entry,
                      Consumer<ScriptEntry> onRun,
                      Consumer<ScriptEntry> onEdit,
                      Consumer<ScriptEntry> onDelete) {
        this.entry = entry;
        this.onRun = onRun;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.statusLabel = new Label();

        initializeCard();
    }

    /**
     * INITIALIZE CARD VISUAL ELEMENTS
     */
    private void initializeCard() {
        // Spacing between elements inside card
        setSpacing(8);
        setPadding(new Insets(15));

        // === CARD STYLING ===
        // Dark background with gradient and rounded corners; the drop shadow lives in the
        // CSS (StyleManager.getCardStyle()), not here.
        setStyle(StyleManager.getCardStyle());

        // Rasterize the card so the CSS Gaussian blur isn't recomputed every scroll frame.
        setCache(true);
        setCacheHint(CacheHint.SPEED);

        // === ROW WITH NAME AND STATUS ===
        HBox titleBox = createTitleBox();

        // === WORKING DIRECTORY PATH ===
        Label pathLabel = new Label("📁 " + entry.getWorkingDir());
        pathLabel.setStyle(StyleManager.getCardPathStyle()); // Gray italic
        pathLabel.setWrapText(true); // Wrap long paths

        // === COMMAND LIST ===
        VBox commandsBox = createCommandsBox();

        // === CONTROL BUTTONS ===
        HBox buttonBox = createButtonBox();

        // Add all elements in vertical order
        getChildren().addAll(titleBox, pathLabel, commandsBox, buttonBox);

        // Add hover highlight effect
        setupHoverEffect();
    }

    /**
     * ROW WITH NAME AND STATUS INDICATOR
     *
     * Example: "My Script ⚫"
     */
    private HBox createTitleBox() {
        HBox titleBox = new HBox(10); // 10px between name and status
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // === SCRIPT NAME ===
        Label nameLabel = new Label(entry.getName());
        nameLabel.setStyle(StyleManager.getCardTitleStyle()); // Large white text

        // === STATUS INDICATOR ===
        // By default set status to "stopped" (⚫)
        StyleManager.setStoppedStatus(statusLabel);

        titleBox.getChildren().addAll(nameLabel, statusLabel);
        return titleBox;
    }

    /**
     * COMMAND LIST FOR EXECUTION
     *
     * Each command is displayed on a separate line with "▶" prefix
     * Example:
     * ▶ npm install
     * ▶ npm start
     */
    private VBox createCommandsBox() {
        VBox commandsBox = new VBox(3); // 3px between commands

        // Iterate through all script commands
        for (String cmd : entry.getCommands()) {
            Label cmdLabel = new Label("▶ " + cmd);
            cmdLabel.setStyle(StyleManager.getCardCommandStyle()); // Monospace font
            commandsBox.getChildren().add(cmdLabel);
        }

        return commandsBox;
    }

    /**
     * CONTROL BUTTON PANEL
     *
     * Three buttons:
     * [▶ RUN]    - run script (green-purple)
     * [✎ EDIT]   - edit script (purple)
     * [✖ DELETE] - delete script (red)
     */
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(8); // 8px between buttons
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0)); // Top padding

        // === RUN BUTTON ===
        Button runBtn = StyleManager.createSmallButton("▶ RUN", StyleManager.ACCENT_GREEN);
        runBtn.setOnAction(e -> onRun.accept(entry)); // Calls handleRunScript() in MainWindow

        // === EDIT BUTTON ===
        Button editBtn = StyleManager.createSmallButton("✎ EDIT", StyleManager.ACCENT_BLUE);
        editBtn.setOnAction(e -> onEdit.accept(entry)); // Opens edit dialog

        // === DELETE BUTTON ===
        Button deleteBtn = StyleManager.createSmallButton("✖ DELETE", StyleManager.ACCENT_RED);
        deleteBtn.setOnAction(e -> onDelete.accept(entry)); // Opens confirmation dialog

        buttonBox.getChildren().addAll(runBtn, editBtn, deleteBtn);
        return buttonBox;
    }

    /**
     * MOUSE HOVER EFFECT
     *
     * On hover: brighter background + purple border + stronger shadow
     * On exit: return to normal appearance
     */
    private void setupHoverEffect() {
        setOnMouseEntered(e -> setStyle(StyleManager.getCardHoverStyle()));
        setOnMouseExited(e -> setStyle(StyleManager.getCardStyle()));
    }

    // === GETTERS FOR ELEMENT ACCESS ===

    /**
     * Get status indicator (⚫/🟢)
     * Used to update status when script starts/stops
     */
    public Label getStatusLabel() {
        return statusLabel;
    }

    /**
     * Get script data
     */
    public ScriptEntry getEntry() {
        return entry;
    }
}