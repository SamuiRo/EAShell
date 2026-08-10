package com.eashell;

import com.eashell.ui.MainWindow;
import com.eashell.util.SingleInstanceLock;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * EA Shell - Developer Script Launcher
 * Main application entry point
 */
public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        if (!SingleInstanceLock.tryAcquire()) {
            new Alert(Alert.AlertType.WARNING,
                    "EAShell is already running. Only one instance can run at a time.")
                    .showAndWait();
            Platform.exit();
            return;
        }

        try {
            Image icon = new Image(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream("/app.png")
                    )
            );

            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Unable to load icon: " + e.getMessage());
            e.printStackTrace();
        }

        MainWindow mainWindow = new MainWindow(primaryStage);
        mainWindow.show();
    }

    @Override
    public void stop() {
        SingleInstanceLock.release();
    }
}