package com.eashell.ui.dialogs;

import com.eashell.model.ScriptEntry;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class DeleteConfirmDialog {

    public static boolean confirm(ScriptEntry entry) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete script: " + entry.getName());
        alert.setContentText("Are you sure you want to delete this script?\nThis action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void showAlreadyRunning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Already Running");
        alert.setHeaderText("Script is already running");
        alert.setContentText("Please stop the existing process first.");
        alert.showAndWait();
    }
}