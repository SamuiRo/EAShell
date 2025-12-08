package com.eashell.ui;

import com.eashell.model.ScriptEntry;
import com.eashell.model.ScriptRepository;
import com.eashell.service.ProcessRunner;
import com.eashell.ui.components.OutputPanel;
import com.eashell.ui.components.ScriptListPanel;
import com.eashell.ui.components.TopBar;
import com.eashell.ui.dialogs.DeleteConfirmDialog;
import com.eashell.ui.dialogs.ScriptDialog;
import com.eashell.util.Constants;
import com.eashell.util.StyleManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainWindow {
    private final Stage primaryStage;
    private final ScriptRepository repository;
    private final Map<String, ProcessRunner> runningProcesses;
    private final ExecutorService executorService;

    private ScriptListPanel scriptListPanel;
    private OutputPanel outputPanel;

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.repository = new ScriptRepository();
        this.runningProcesses = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public void show() {
        primaryStage.setTitle(Constants.APP_TITLE);

        BorderPane root = new BorderPane();
        root.setStyle(StyleManager.getRootStyle());

        TopBar topBar = new TopBar(
                this::handleAddScript,
                this::handleStopAll,
                runningProcesses::size
        );
        root.setTop(topBar);

        SplitPane splitPane = new SplitPane();
        splitPane.setStyle(StyleManager.getSplitPaneStyle());

        scriptListPanel = new ScriptListPanel(
                this::handleRunScript,
                this::handleEditScript,
                this::handleDeleteScript
        );

        outputPanel = new OutputPanel();

        splitPane.getItems().addAll(scriptListPanel, outputPanel);
        splitPane.setDividerPositions(Constants.SPLIT_PANE_DIVIDER_POSITION);

        root.setCenter(splitPane);

        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        scene.getStylesheets().add(StyleManager.getStylesheet());
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> cleanup());

        refreshScriptList();
        primaryStage.show();
    }

    private void handleAddScript() {
        ScriptDialog.showAddDialog().ifPresent(entry -> {
            repository.add(entry);
            refreshScriptList();
        });
    }

    private void handleEditScript(ScriptEntry entry) {
        ScriptDialog.showEditDialog(entry).ifPresent(newEntry -> {
            repository.update(entry, newEntry);
            refreshScriptList();
        });
    }

    private void handleDeleteScript(ScriptEntry entry) {
        if (DeleteConfirmDialog.confirm(entry)) {
            repository.remove(entry);
            refreshScriptList();
        }
    }

    private void handleRunScript(ScriptEntry entry) {
        if (runningProcesses.containsKey(entry.getName())) {
            DeleteConfirmDialog.showAlreadyRunning();
            return;
        }

        Tab outputTab = outputPanel.createOutputTab(
                entry,
                null, // Temporary, will be replaced
                this::updateScriptStatus
        );

        TextArea outputArea = outputPanel.getOutputAreaFromTab(outputTab);
        ProcessRunner runner = new ProcessRunner(entry, outputArea, outputTab);

        runningProcesses.put(entry.getName(), runner);
        updateScriptStatus(entry.getName(), true);

        executorService.submit(runner);
    }

    private void handleStopAll() {
        runningProcesses.values().forEach(ProcessRunner::stop);
        runningProcesses.clear();

        scriptListPanel.getStatusLabels().forEach((name, label) ->
                StyleManager.setStoppedStatus(label)
        );
    }

    private void updateScriptStatus(String scriptName, boolean running) {
        Platform.runLater(() -> {
            scriptListPanel.updateScriptStatus(scriptName, running);
            if (!running) {
                runningProcesses.remove(scriptName);
            }
        });
    }

    private void refreshScriptList() {
        scriptListPanel.refresh(repository.getAll());
    }

    private void cleanup() {
        handleStopAll();
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(Constants.EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}