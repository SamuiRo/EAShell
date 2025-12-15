package com.eashell;

import com.eashell.ui.MainWindow;
import javafx.application.Application;
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
}