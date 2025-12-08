package com.eashell.ui.components;

import com.eashell.model.ScriptEntry;
import com.eashell.util.StyleManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

import java.util.function.Consumer;

public class ScriptCard extends VBox {
    private final ScriptEntry entry;
    private final Label statusLabel;
    private final Consumer<ScriptEntry> onRun;
    private final Consumer<ScriptEntry> onEdit;
    private final Consumer<ScriptEntry> onDelete;

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

    private void initializeCard() {
        setSpacing(8);
        setPadding(new Insets(15));
        setStyle(StyleManager.getCardStyle());

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 255, 65, 0.3));
        setEffect(shadow);

        // Title with status indicator
        HBox titleBox = createTitleBox();

        // Path
        Label pathLabel = new Label("📁 " + entry.getWorkingDir());
        pathLabel.setStyle(StyleManager.getCardPathStyle());
        pathLabel.setWrapText(true);

        // Commands
        VBox commandsBox = createCommandsBox();

        // Buttons
        HBox buttonBox = createButtonBox();

        getChildren().addAll(titleBox, pathLabel, commandsBox, buttonBox);

        setupHoverEffect();
    }

    private HBox createTitleBox() {
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(entry.getName());
        nameLabel.setStyle(StyleManager.getCardTitleStyle());

        StyleManager.setStoppedStatus(statusLabel);

        titleBox.getChildren().addAll(nameLabel, statusLabel);
        return titleBox;
    }

    private VBox createCommandsBox() {
        VBox commandsBox = new VBox(3);
        for (String cmd : entry.getCommands()) {
            Label cmdLabel = new Label("▶ " + cmd);
            cmdLabel.setStyle(StyleManager.getCardCommandStyle());
            commandsBox.getChildren().add(cmdLabel);
        }
        return commandsBox;
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));

        Button runBtn = StyleManager.createSmallButton("▶ RUN", StyleManager.ACCENT_GREEN);
        runBtn.setOnAction(e -> onRun.accept(entry));

        Button editBtn = StyleManager.createSmallButton("✎ EDIT", StyleManager.ACCENT_BLUE);
        editBtn.setOnAction(e -> onEdit.accept(entry));

        Button deleteBtn = StyleManager.createSmallButton("✖ DELETE", StyleManager.ACCENT_RED);
        deleteBtn.setOnAction(e -> onDelete.accept(entry));

        buttonBox.getChildren().addAll(runBtn, editBtn, deleteBtn);
        return buttonBox;
    }

    private void setupHoverEffect() {
        setOnMouseEntered(e -> setStyle(StyleManager.getCardHoverStyle()));
        setOnMouseExited(e -> setStyle(StyleManager.getCardStyle()));
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    public ScriptEntry getEntry() {
        return entry;
    }
}