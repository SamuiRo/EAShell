package com.eashell;

import com.eashell.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

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
        MainWindow mainWindow = new MainWindow(primaryStage);
        mainWindow.show();
    }
}