package com.eashell.ui.dialogs;

import com.eashell.model.ScriptEntry;
import com.eashell.util.StyleManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScriptDialog {

    public static Optional<ScriptEntry> showAddDialog() {
        return showDialog("Add New Script", "Configure your script", null);
    }

    public static Optional<ScriptEntry> showEditDialog(ScriptEntry existingEntry) {
        return showDialog("Edit Script", "Modify script configuration", existingEntry);
    }

    private static Optional<ScriptEntry> showDialog(String title, String header, ScriptEntry existingEntry) {
        Dialog<ScriptEntry> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(StyleManager.getDialogStyle());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = createFormGrid(dialog, existingEntry);
        dialogPane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return extractScriptEntry(grid);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static GridPane createFormGrid(Dialog<ScriptEntry> dialog, ScriptEntry existingEntry) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Name field
        TextField nameField = new TextField();
        nameField.setPromptText("Script Name");
        if (existingEntry != null) {
            nameField.setText(existingEntry.getName());
        }
        StyleManager.styleTextField(nameField);

        // Path field
        TextField pathField = new TextField();
        pathField.setPromptText("Working Directory");
        if (existingEntry != null) {
            pathField.setText(existingEntry.getWorkingDir());
        }
        StyleManager.styleTextField(pathField);

        // Browse button
        Button browseBtn = StyleManager.createSmallButton("Browse", StyleManager.ACCENT_BLUE);
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Working Directory");
            File dir = dc.showDialog(dialog.getOwner());
            if (dir != null) {
                pathField.setText(dir.getAbsolutePath());
            }
        });

        // Commands area
        TextArea commandsArea = new TextArea();
        commandsArea.setPromptText("Commands (one per line)\nExample:\nnpm install\nnode index.js");
        commandsArea.setPrefRowCount(5);
        if (existingEntry != null) {
            commandsArea.setText(String.join("\n", existingEntry.getCommands()));
        }
        StyleManager.styleTextArea(commandsArea);

        // Add to grid
        grid.add(StyleManager.createLabel("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(StyleManager.createLabel("Path:"), 0, 1);
        grid.add(pathField, 1, 1);
        grid.add(browseBtn, 2, 1);
        grid.add(StyleManager.createLabel("Commands:"), 0, 2);
        grid.add(commandsArea, 1, 2, 2, 1);

        // Store references for extraction
        grid.setUserData(new FormData(nameField, pathField, commandsArea));

        return grid;
    }

    private static ScriptEntry extractScriptEntry(GridPane grid) {
        FormData data = (FormData) grid.getUserData();

        String name = data.nameField.getText().trim();
        String path = data.pathField.getText().trim();

        String[] cmdLines = data.commandsArea.getText().split("\n");
        List<String> commands = new ArrayList<>();
        for (String cmd : cmdLines) {
            String trimmed = cmd.trim();
            if (!trimmed.isEmpty()) {
                commands.add(trimmed);
            }
        }

        return new ScriptEntry(name, path, commands);
    }

    private static class FormData {
        final TextField nameField;
        final TextField pathField;
        final TextArea commandsArea;

        FormData(TextField nameField, TextField pathField, TextArea commandsArea) {
            this.nameField = nameField;
            this.pathField = pathField;
            this.commandsArea = commandsArea;
        }
    }
}