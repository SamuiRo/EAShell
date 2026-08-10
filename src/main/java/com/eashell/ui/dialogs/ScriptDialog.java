package com.eashell.ui.dialogs;

import com.eashell.model.ScriptEntry;
import com.eashell.util.Constants;
import com.eashell.util.StyleManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScriptDialog {

    public static Optional<ScriptEntry> showAddDialog(List<ScriptEntry> existingEntries) {
        return showDialog("Add New Script", "Configure your script", null, existingEntries);
    }

    public static Optional<ScriptEntry> showEditDialog(ScriptEntry existingEntry, List<ScriptEntry> existingEntries) {
        return showDialog("Edit Script", "Modify script configuration", existingEntry, existingEntries);
    }

    private static Optional<ScriptEntry> showDialog(String title, String header, ScriptEntry existingEntry,
                                                      List<ScriptEntry> existingEntries) {
        Dialog<ScriptEntry> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        DialogPane dialogPane = dialog.getDialogPane();
        // A Dialog owns its own Scene, so it never inherits the main window's stylesheet -
        // it must be attached here too, or the dialog falls back to plain default styling.
        dialogPane.getStylesheets().add(ScriptDialog.class.getResource("/styles/app.css").toExternalForm());
        dialogPane.getStyleClass().add("app-dialog");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        // The dialog's built-in OK/Cancel buttons aren't created by StyleManager's factory
        // methods, so they need their role classes added directly here.
        dialogPane.lookupButton(ButtonType.OK).getStyleClass().addAll("btn-large", StyleManager.BTN_ACCENT);
        dialogPane.lookupButton(ButtonType.CANCEL).getStyleClass().addAll("btn-large", StyleManager.BTN_NEUTRAL);

        GridPane grid = createFormGrid(dialog, existingEntry, existingEntries);
        dialogPane.setContent(grid);

        wireValidation(dialogPane, grid, existingEntry, existingEntries);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return extractScriptEntry(grid, existingEntry);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Disables OK until the name and path are non-empty, the path is a real directory, the
     * name isn't already used by another script (renaming a script to its own current name is
     * fine), and there's at least one non-blank command line - an empty command list saves
     * fine otherwise and, when run, immediately reports success having done nothing.
     */
    private static void wireValidation(DialogPane dialogPane, GridPane grid, ScriptEntry existingEntry,
                                        List<ScriptEntry> existingEntries) {
        FormData data = (FormData) grid.getUserData();
        Node okButton = dialogPane.lookupButton(ButtonType.OK);

        Runnable validate = () -> {
            String name = data.nameField.getText().trim();
            String path = data.pathField.getText().trim();

            boolean pathIsDirectory;
            try {
                pathIsDirectory = !path.isEmpty() && Files.isDirectory(Path.of(path));
            } catch (InvalidPathException e) {
                pathIsDirectory = false;
            }

            boolean nameTaken = existingEntries.stream().anyMatch(e ->
                    e.getName().equals(name) && (existingEntry == null || !e.getId().equals(existingEntry.getId())));

            boolean hasCommand = data.commandsArea.getText().lines().anyMatch(line -> !line.isBlank());

            okButton.setDisable(name.isEmpty() || !pathIsDirectory || nameTaken || !hasCommand);
        };

        data.nameField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
        data.pathField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
        data.commandsArea.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
        validate.run();
    }

    private static GridPane createFormGrid(Dialog<ScriptEntry> dialog, ScriptEntry existingEntry,
                                            List<ScriptEntry> existingEntries) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Name field - styled by the .text-field rule in app.css (default style class)
        TextField nameField = new TextField();
        nameField.setPromptText("Script Name");
        if (existingEntry != null) {
            nameField.setText(existingEntry.getName());
        }

        // Path field
        TextField pathField = new TextField();
        pathField.setPromptText("Working Directory");
        if (existingEntry != null) {
            pathField.setText(existingEntry.getWorkingDir());
        }

        // Browse button
        Button browseBtn = StyleManager.createSmallButton("Browse", StyleManager.BTN_NEUTRAL);
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Working Directory");
            File dir = dc.showDialog(dialog.getOwner());
            if (dir != null) {
                pathField.setText(dir.getAbsolutePath());
            }
        });

        // Group field - editable combo: pick an existing group or type a new one
        ComboBox<String> groupField = new ComboBox<>();
        groupField.setEditable(true);
        groupField.setPromptText(Constants.GROUP_FIELD_PROMPT);
        List<String> distinctGroups = existingEntries.stream()
                .map(ScriptEntry::getGroup)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        groupField.getItems().addAll(distinctGroups);
        if (existingEntry != null && existingEntry.getGroup() != null) {
            groupField.setValue(existingEntry.getGroup());
        }

        // Commands area
        TextArea commandsArea = new TextArea();
        commandsArea.setPromptText("Commands (one per line)\nExample:\nnpm install\nnode index.js");
        commandsArea.setPrefRowCount(5);
        if (existingEntry != null) {
            commandsArea.setText(String.join("\n", existingEntry.getCommands()));
        }

        // Add to grid
        grid.add(StyleManager.createLabel("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(StyleManager.createLabel("Path:"), 0, 1);
        grid.add(pathField, 1, 1);
        grid.add(browseBtn, 2, 1);
        grid.add(StyleManager.createLabel("Group:"), 0, 2);
        grid.add(groupField, 1, 2, 2, 1);
        grid.add(StyleManager.createLabel("Commands:"), 0, 3);
        grid.add(commandsArea, 1, 3, 2, 1);

        // Store references for extraction
        grid.setUserData(new FormData(nameField, pathField, groupField, commandsArea));

        return grid;
    }

    private static ScriptEntry extractScriptEntry(GridPane grid, ScriptEntry existingEntry) {
        FormData data = (FormData) grid.getUserData();

        String name = data.nameField.getText().trim();
        String path = data.pathField.getText().trim();
        // Read the editor directly rather than getValue(), which only reflects committed
        // text and may not have updated yet at the moment OK is clicked.
        String groupText = data.groupField.getEditor().getText().trim();
        String group = groupText.isEmpty() ? null : groupText;

        String[] cmdLines = data.commandsArea.getText().split("\n");
        List<String> commands = new ArrayList<>();
        for (String cmd : cmdLines) {
            String trimmed = cmd.trim();
            if (!trimmed.isEmpty()) {
                commands.add(trimmed);
            }
        }

        // Editing must keep the original id - runningProcesses/scriptCards are keyed by it,
        // and minting a new one here would orphan a runner the same way renaming used to.
        ScriptEntry entry = existingEntry != null
                ? new ScriptEntry(existingEntry.getId(), name, path, commands)
                : new ScriptEntry(name, path, commands);
        entry.setGroup(group);
        return entry;
    }

    private static class FormData {
        final TextField nameField;
        final TextField pathField;
        final ComboBox<String> groupField;
        final TextArea commandsArea;

        FormData(TextField nameField, TextField pathField, ComboBox<String> groupField, TextArea commandsArea) {
            this.nameField = nameField;
            this.pathField = pathField;
            this.groupField = groupField;
            this.commandsArea = commandsArea;
        }
    }
}