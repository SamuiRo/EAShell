package com.eashell;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class App extends Application {
    private static final String DATA_FILE = "eashell_data.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_BUFFER_SIZE = 10000; // Максимум символів в буфері

    private List<ScriptEntry> entries = new ArrayList<>();
    private VBox scriptListContainer;
    private Map<String, ProcessRunner> runningProcesses = new ConcurrentHashMap<>();
    private TabPane outputTabPane;
    private ExecutorService executorService;
    private Map<String, Label> statusLabels = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Створюємо пул потоків для виконання процесів
        executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // Daemon потоки автоматично завершаться при закритті програми
            return t;
        });

        loadEntries();

        primaryStage.setTitle("EA Terminal");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0e27;");

        HBox topBar = createTopBar();
        root.setTop(topBar);

        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #0a0e27;");

        VBox leftPanel = createScriptListPanel();
        VBox rightPanel = createOutputPanel();

        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.4);

        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1400, 800);
        scene.getStylesheets().add(getStylesheet());
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            stopAllProcesses();
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            saveEntries();
        });
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: linear-gradient(to right, #1a1f3a, #0f1729); " +
                "-fx-border-color: #00ff41; -fx-border-width: 0 0 2 0;");

        Label title = new Label("⚡ DEV LAUNCHER");
        title.setStyle("-fx-text-fill: #00ff41; -fx-font-size: 24px; -fx-font-weight: bold; " +
                "-fx-font-family: 'Consolas', 'Courier New', monospace;");

        Label statusLabel = new Label("Running: 0");
        statusLabel.setId("global-status");
        statusLabel.setStyle("-fx-text-fill: #5af; -fx-font-size: 14px; " +
                "-fx-font-family: 'Consolas', monospace;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = createStyledButton("+ NEW SCRIPT", "#00ff41");
        addButton.setOnAction(e -> showAddScriptDialog());

        Button stopAllButton = createStyledButton("⏹ STOP ALL", "#ff4444");
        stopAllButton.setOnAction(e -> stopAllProcesses());

        topBar.getChildren().addAll(title, statusLabel, spacer, addButton, stopAllButton);

        // Оновлюємо лічильник кожну секунду
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int running = runningProcesses.size();
                Platform.runLater(() -> statusLabel.setText("Running: " + running));
            }
        }, 0, 1000);

        return topBar;
    }

    private VBox createScriptListPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #0f1729;");

        Label header = new Label("📋 SAVED SCRIPTS");
        header.setStyle("-fx-text-fill: #00ff41; -fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-font-family: 'Consolas', monospace;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0f1729; -fx-background-color: #0f1729;");

        scriptListContainer = new VBox(10);
        scriptListContainer.setPadding(new Insets(10));
        scrollPane.setContent(scriptListContainer);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        panel.getChildren().addAll(header, new Separator(), scrollPane);

        refreshScriptList();
        return panel;
    }

    private VBox createOutputPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #0a0e27;");

        Label header = new Label("📟 OUTPUT CONSOLE");
        header.setStyle("-fx-text-fill: #00ff41; -fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-font-family: 'Consolas', monospace;");

        outputTabPane = new TabPane();
        outputTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        outputTabPane.setStyle("-fx-background-color: #0f1729;");

        VBox.setVgrow(outputTabPane, Priority.ALWAYS);
        panel.getChildren().addAll(header, new Separator(), outputTabPane);

        return panel;
    }

    private void refreshScriptList() {
        scriptListContainer.getChildren().clear();

        if (entries.isEmpty()) {
            Label emptyLabel = new Label("No scripts added yet.\nClick 'NEW SCRIPT' to get started.");
            emptyLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-text-alignment: center;");
            emptyLabel.setAlignment(Pos.CENTER);
            scriptListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (ScriptEntry entry : entries) {
            scriptListContainer.getChildren().add(createScriptCard(entry));
        }
    }

    private VBox createScriptCard(ScriptEntry entry) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #1a1f3a; -fx-background-radius: 8; " +
                "-fx-border-color: #2a3f5f; -fx-border-width: 1; -fx-border-radius: 8;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 255, 65, 0.3));
        card.setEffect(shadow);

        // Title з індикатором статусу
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(entry.name);
        nameLabel.setStyle("-fx-text-fill: #00ff41; -fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-font-family: 'Consolas', monospace;");

        Label statusLabel = new Label("⚫");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        statusLabels.put(entry.name, statusLabel);

        titleBox.getChildren().addAll(nameLabel, statusLabel);

        // Path
        Label pathLabel = new Label("📁 " + entry.workingDir);
        pathLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
        pathLabel.setWrapText(true);

        // Commands
        VBox commandsBox = new VBox(3);
        for (String cmd : entry.commands) {
            Label cmdLabel = new Label("▶ " + cmd);
            cmdLabel.setStyle("-fx-text-fill: #5af; -fx-font-size: 12px; " +
                    "-fx-font-family: 'Consolas', monospace;");
            commandsBox.getChildren().add(cmdLabel);
        }

        // Buttons
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));

        Button runBtn = createSmallButton("▶ RUN", "#00ff41");
        runBtn.setOnAction(e -> runScript(entry));

        Button editBtn = createSmallButton("✎ EDIT", "#5af");
        editBtn.setOnAction(e -> showEditScriptDialog(entry));

        Button deleteBtn = createSmallButton("✖ DELETE", "#ff4444");
        deleteBtn.setOnAction(e -> deleteScript(entry));

        buttonBox.getChildren().addAll(runBtn, editBtn, deleteBtn);

        card.getChildren().addAll(titleBox, pathLabel, commandsBox, buttonBox);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #232a4a; -fx-background-radius: 8; " +
                        "-fx-border-color: #00ff41; -fx-border-width: 1; -fx-border-radius: 8;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #1a1f3a; -fx-background-radius: 8; " +
                        "-fx-border-color: #2a3f5f; -fx-border-width: 1; -fx-border-radius: 8;"));

        return card;
    }

    private void showAddScriptDialog() {
        Dialog<ScriptEntry> dialog = new Dialog<>();
        dialog.setTitle("Add New Script");
        dialog.setHeaderText("Configure your script");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1a1f3a;");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Script Name");
        styleTextField(nameField);

        TextField pathField = new TextField();
        pathField.setPromptText("Working Directory");
        styleTextField(pathField);

        Button browseBtn = createSmallButton("Browse", "#5af");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File dir = dc.showDialog(dialog.getOwner());
            if (dir != null) pathField.setText(dir.getAbsolutePath());
        });

        TextArea commandsArea = new TextArea();
        commandsArea.setPromptText("Commands (one per line)\nExample:\nnpm install\nnode index.js");
        commandsArea.setPrefRowCount(5);
        styleTextArea(commandsArea);

        grid.add(createLabel("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(createLabel("Path:"), 0, 1);
        grid.add(pathField, 1, 1);
        grid.add(browseBtn, 2, 1);
        grid.add(createLabel("Commands:"), 0, 2);
        grid.add(commandsArea, 1, 2, 2, 1);

        dialogPane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String[] cmds = commandsArea.getText().split("\n");
                List<String> commands = new ArrayList<>();
                for (String cmd : cmds) {
                    if (!cmd.trim().isEmpty()) commands.add(cmd.trim());
                }
                return new ScriptEntry(nameField.getText(), pathField.getText(), commands);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(entry -> {
            entries.add(entry);
            saveEntries();
            refreshScriptList();
        });
    }

    private void showEditScriptDialog(ScriptEntry entry) {
        Dialog<ScriptEntry> dialog = new Dialog<>();
        dialog.setTitle("Edit Script");
        dialog.setHeaderText("Modify script configuration");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1a1f3a;");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(entry.name);
        styleTextField(nameField);

        TextField pathField = new TextField(entry.workingDir);
        styleTextField(pathField);

        Button browseBtn = createSmallButton("Browse", "#5af");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File dir = dc.showDialog(dialog.getOwner());
            if (dir != null) pathField.setText(dir.getAbsolutePath());
        });

        TextArea commandsArea = new TextArea(String.join("\n", entry.commands));
        commandsArea.setPrefRowCount(5);
        styleTextArea(commandsArea);

        grid.add(createLabel("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(createLabel("Path:"), 0, 1);
        grid.add(pathField, 1, 1);
        grid.add(browseBtn, 2, 1);
        grid.add(createLabel("Commands:"), 0, 2);
        grid.add(commandsArea, 1, 2, 2, 1);

        dialogPane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String[] cmds = commandsArea.getText().split("\n");
                List<String> commands = new ArrayList<>();
                for (String cmd : cmds) {
                    if (!cmd.trim().isEmpty()) commands.add(cmd.trim());
                }
                return new ScriptEntry(nameField.getText(), pathField.getText(), commands);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newEntry -> {
            entry.name = newEntry.name;
            entry.workingDir = newEntry.workingDir;
            entry.commands = newEntry.commands;
            saveEntries();
            refreshScriptList();
        });
    }

    private void deleteScript(ScriptEntry entry) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete script: " + entry.name);
        alert.setContentText("Are you sure you want to delete this script?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                entries.remove(entry);
                saveEntries();
                refreshScriptList();
            }
        });
    }

    private void runScript(ScriptEntry entry) {
        // Перевіряємо чи вже запущений
        if (runningProcesses.containsKey(entry.name)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Already Running");
            alert.setHeaderText("Script is already running");
            alert.setContentText("Please stop the existing process first.");
            alert.showAndWait();
            return;
        }

        Tab outputTab = new Tab(entry.name + " 🟢");
        outputTab.setClosable(true);

        VBox tabContent = new VBox(5);
        tabContent.setPadding(new Insets(10));
        tabContent.setStyle("-fx-background-color: #0a0e27;");

        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle("-fx-control-inner-background: #000; -fx-text-fill: #0f0; " +
                "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        HBox controlBox = new HBox(8);
        Button stopBtn = createSmallButton("⏹ STOP", "#ff4444");
        Button clearBtn = createSmallButton("🗑 CLEAR", "#5af");

        stopBtn.setOnAction(e -> {
            ProcessRunner runner = runningProcesses.get(entry.name);
            if (runner != null) runner.stop();
        });

        clearBtn.setOnAction(e -> outputArea.clear());

        controlBox.getChildren().addAll(stopBtn, clearBtn);

        tabContent.getChildren().addAll(outputArea, controlBox);
        outputTab.setContent(tabContent);
        outputTabPane.getTabs().add(outputTab);
        outputTabPane.getSelectionModel().select(outputTab);

        ProcessRunner runner = new ProcessRunner(entry, outputArea, outputTab);
        runningProcesses.put(entry.name, runner);

        // Оновлюємо статус в картці
        updateScriptStatus(entry.name, true);

        outputTab.setOnClosed(e -> {
            runner.stop();
            runningProcesses.remove(entry.name);
            updateScriptStatus(entry.name, false);
        });

        executorService.submit(runner);
    }

    private void updateScriptStatus(String scriptName, boolean running) {
        Platform.runLater(() -> {
            Label statusLabel = statusLabels.get(scriptName);
            if (statusLabel != null) {
                if (running) {
                    statusLabel.setText("🟢");
                    statusLabel.setStyle("-fx-text-fill: #0f0;");
                } else {
                    statusLabel.setText("⚫");
                    statusLabel.setStyle("-fx-text-fill: #666;");
                }
            }
        });
    }

    private void stopAllProcesses() {
        runningProcesses.values().forEach(ProcessRunner::stop);
        runningProcesses.clear();
        statusLabels.values().forEach(label -> {
            label.setText("⚫");
            label.setStyle("-fx-text-fill: #666;");
        });
    }

    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #000; " +
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; " +
                "-fx-background-radius: 5; -fx-font-family: 'Consolas', monospace;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: derive(" + color + ", 20%); -fx-text-fill: #000; " +
                        "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; " +
                        "-fx-background-radius: 5; -fx-font-family: 'Consolas', monospace;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + color + "; -fx-text-fill: #000; " +
                        "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; " +
                        "-fx-background-radius: 5; -fx-font-family: 'Consolas', monospace;"));
        return btn;
    }

    private Button createSmallButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #000; " +
                "-fx-font-size: 10px; -fx-padding: 5 10; -fx-background-radius: 3; " +
                "-fx-font-family: 'Consolas', monospace;");
        return btn;
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #00ff41; -fx-font-family: 'Consolas', monospace;");
        return lbl;
    }

    private void styleTextField(TextField field) {
        field.setStyle("-fx-background-color: #0a0e27; -fx-text-fill: #0f0; " +
                "-fx-border-color: #00ff41; -fx-border-radius: 3; " +
                "-fx-font-family: 'Consolas', monospace;");
    }

    private void styleTextArea(TextArea area) {
        area.setStyle("-fx-control-inner-background: #0a0e27; -fx-text-fill: #0f0; " +
                "-fx-border-color: #00ff41; -fx-font-family: 'Consolas', monospace;");
    }

    private void saveEntries() {
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            GSON.toJson(entries, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadEntries() {
        try {
            if (Files.exists(Paths.get(DATA_FILE))) {
                String json = new String(Files.readAllBytes(Paths.get(DATA_FILE)));
                entries = GSON.fromJson(json, new TypeToken<List<ScriptEntry>>(){}.getType());
                if (entries == null) entries = new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            entries = new ArrayList<>();
        }
    }

    private String getStylesheet() {
        return "data:text/css," +
                ".scroll-pane { -fx-background-color: transparent; }" +
                ".scroll-pane .viewport { -fx-background-color: transparent; }" +
                ".scroll-pane .content { -fx-background-color: transparent; }";
    }

    static class ScriptEntry {
        String name;
        String workingDir;
        List<String> commands;

        public ScriptEntry(String name, String workingDir, List<String> commands) {
            this.name = name;
            this.workingDir = workingDir;
            this.commands = commands;
        }
    }

    static class ProcessRunner implements Runnable {
        private final ScriptEntry entry;
        private final TextArea outputArea;
        private final Tab tab;
        private Process process;
        private volatile boolean running = true;
        private final StringBuilder outputBuffer = new StringBuilder();
        private long lastUIUpdate = 0;
        private static final long UI_UPDATE_INTERVAL = 100; // мс

        public ProcessRunner(ScriptEntry entry, TextArea outputArea, Tab tab) {
            this.entry = entry;
            this.outputArea = outputArea;
            this.tab = tab;
        }

        @Override
        public void run() {
            try {
                for (String command : entry.commands) {
                    if (!running) break;

                    appendOutput(">>> Executing: " + command + "\n");

                    ProcessBuilder pb = new ProcessBuilder();
                    pb.directory(new File(entry.workingDir));

                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        pb.command("cmd.exe", "/c", command);
                    } else {
                        pb.command("sh", "-c", command);
                    }

                    pb.redirectErrorStream(true);
                    process = pb.start();

                    // Читаємо вивід в окремому потоці
                    Thread readerThread = new Thread(() -> readProcessOutput());
                    readerThread.setDaemon(true);
                    readerThread.start();

                    int exitCode = process.waitFor();

                    // Даємо час reader thread'у завершити роботу
                    readerThread.join(1000);

                    // Фінальне оновлення UI
                    flushBuffer();

                    appendOutput("\n>>> Exit code: " + exitCode + "\n\n");

                    if (!running) break;
                }
                appendOutput(">>> All commands completed.\n");
                Platform.runLater(() -> tab.setText(entry.name + " ✓"));
            } catch (Exception e) {
                appendOutput("\n>>> ERROR: " + e.getMessage() + "\n");
                Platform.runLater(() -> tab.setText(entry.name + " ✗"));
            } finally {
                running = false;
            }
        }

        private void readProcessOutput() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                char[] buffer = new char[8192];
                int charsRead;

                while (running && (charsRead = reader.read(buffer)) != -1) {
                    String chunk = new String(buffer, 0, charsRead);
                    bufferOutput(chunk);
                }

                flushBuffer();
            } catch (IOException e) {
                if (running) {
                    appendOutput("\n>>> Error reading output: " + e.getMessage() + "\n");
                }
            }
        }

        private void bufferOutput(String text) {
            synchronized (outputBuffer) {
                outputBuffer.append(text);

                // Оновлюємо UI пакетами з затримкою
                long now = System.currentTimeMillis();
                if (now - lastUIUpdate > UI_UPDATE_INTERVAL || outputBuffer.length() > 4096) {
                    flushBuffer();
                }
            }
        }

        private void flushBuffer() {
            synchronized (outputBuffer) {
                if (outputBuffer.length() > 0) {
                    String text = outputBuffer.toString();
                    outputBuffer.setLength(0);

                    Platform.runLater(() -> {
                        outputArea.appendText(text);

                        // Обмежуємо розмір тексту в TextArea
                        if (outputArea.getLength() > MAX_BUFFER_SIZE) {
                            outputArea.deleteText(0, outputArea.getLength() - MAX_BUFFER_SIZE);
                        }

                        // Автоскрол вниз
                        outputArea.setScrollTop(Double.MAX_VALUE);
                    });

                    lastUIUpdate = System.currentTimeMillis();
                }
            }
        }

        private void appendOutput(String text) {
            bufferOutput(text);
        }

        public void stop() {
            running = false;
            if (process != null && process.isAlive()) {
                // Спочатку пробуємо graceful shutdown
                process.destroy();

                // Чекаємо 2 секунди
                try {
                    if (!process.waitFor(2, TimeUnit.SECONDS)) {
                        // Якщо не завершився - force kill
                        process.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    process.destroyForcibly();
                    Thread.currentThread().interrupt();
                }

                appendOutput("\n>>> Process terminated by user.\n");
                flushBuffer();
                Platform.runLater(() -> tab.setText(entry.name + " ⏹"));
            }
        }
    }
}