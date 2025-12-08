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

public class ScriptListPanel extends VBox {
    private final VBox scriptListContainer;
    private final Map<String, ScriptCard> scriptCards;
    private final Consumer<ScriptEntry> onRun;
    private final Consumer<ScriptEntry> onEdit;
    private final Consumer<ScriptEntry> onDelete;

    public ScriptListPanel(Consumer<ScriptEntry> onRun,
                           Consumer<ScriptEntry> onEdit,
                           Consumer<ScriptEntry> onDelete) {
        this.scriptCards = new HashMap<>();
        this.onRun = onRun;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        setSpacing(10);
        setPadding(new Insets(20));
        setStyle(StyleManager.getPanelStyle());

        Label header = new Label(Constants.SCRIPTS_HEADER);
        header.setStyle(StyleManager.getHeaderStyle());

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(StyleManager.getScrollPaneStyle());

        scriptListContainer = new VBox(10);
        scriptListContainer.setPadding(new Insets(10));
        scrollPane.setContent(scriptListContainer);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().addAll(header, new Separator(), scrollPane);
    }

    public void refresh(List<ScriptEntry> entries) {
        scriptListContainer.getChildren().clear();
        scriptCards.clear();

        if (entries.isEmpty()) {
            showEmptyMessage();
            return;
        }

        for (ScriptEntry entry : entries) {
            ScriptCard card = new ScriptCard(entry, onRun, onEdit, onDelete);
            scriptCards.put(entry.getName(), card);
            scriptListContainer.getChildren().add(card);
        }
    }

    private void showEmptyMessage() {
        Label emptyLabel = new Label("No scripts added yet.\nClick 'NEW SCRIPT' to get started.");
        emptyLabel.setStyle(StyleManager.getEmptyLabelStyle());
        emptyLabel.setAlignment(Pos.CENTER);
        scriptListContainer.getChildren().add(emptyLabel);
    }

    public void updateScriptStatus(String scriptName, boolean running) {
        ScriptCard card = scriptCards.get(scriptName);
        if (card != null) {
            if (running) {
                StyleManager.setRunningStatus(card.getStatusLabel());
            } else {
                StyleManager.setStoppedStatus(card.getStatusLabel());
            }
        }
    }

    public Map<String, Label> getStatusLabels() {
        Map<String, Label> labels = new HashMap<>();
        scriptCards.forEach((name, card) -> labels.put(name, card.getStatusLabel()));
        return labels;
    }
}