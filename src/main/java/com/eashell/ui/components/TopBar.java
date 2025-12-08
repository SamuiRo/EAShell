package com.eashell.ui.components;

import com.eashell.util.Constants;
import com.eashell.util.StyleManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

public class TopBar extends HBox {
    private final Label statusLabel;
    private final Runnable onAddScript;
    private final Runnable onStopAll;
    private final Supplier<Integer> runningCountSupplier;

    public TopBar(Runnable onAddScript, Runnable onStopAll, Supplier<Integer> runningCountSupplier) {
        this.onAddScript = onAddScript;
        this.onStopAll = onStopAll;
        this.runningCountSupplier = runningCountSupplier;
        this.statusLabel = new Label("Running: 0");

        initialize();
        startStatusUpdater();
    }

    private void initialize() {
        setSpacing(20);
        setPadding(new Insets(15, 20, 15, 20));
        setAlignment(Pos.CENTER_LEFT);
        setStyle(StyleManager.getTopBarStyle());

        Label title = new Label(Constants.TITLE_LABEL);
        title.setStyle(StyleManager.getTitleStyle());

        statusLabel.setId("global-status");
        statusLabel.setStyle(StyleManager.getStatusLabelStyle());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = StyleManager.createStyledButton("+ NEW SCRIPT", StyleManager.ACCENT_GREEN);
        addButton.setOnAction(e -> onAddScript.run());

        Button stopAllButton = StyleManager.createStyledButton("⏹ STOP ALL", StyleManager.ACCENT_RED);
        stopAllButton.setOnAction(e -> onStopAll.run());

        getChildren().addAll(title, statusLabel, spacer, addButton, stopAllButton);
    }

    private void startStatusUpdater() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int running = runningCountSupplier.get();
                Platform.runLater(() -> statusLabel.setText("Running: " + running));
            }
        }, 0, 1000);
    }
}