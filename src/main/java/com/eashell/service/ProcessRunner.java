package com.eashell.service;

import com.eashell.model.ScriptEntry;
import com.eashell.util.Constants;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ProcessRunner implements Runnable {
    private final ScriptEntry entry;
    private TextArea outputArea;
    private Tab tab;
    private Process process;
    private volatile boolean running = true;
    private final StringBuilder outputBuffer = new StringBuilder();
    private long lastUIUpdate = 0;

    public ProcessRunner(ScriptEntry entry, TextArea outputArea, Tab tab) {
        this.entry = entry;
        this.outputArea = outputArea;
        this.tab = tab;
    }

    @Override
    public void run() {
        try {
            for (String command : entry.getCommands()) {
                if (!running) break;

                appendOutput(">>> Executing: " + command + "\n");

                ProcessBuilder pb = new ProcessBuilder();
                pb.directory(new File(entry.getWorkingDir()));

                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    // Використовуємо PowerShell замість cmd.exe
//                    pb.command("cmd.exe", "/c", command);
                    pb.command("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", command);
                } else {
                    pb.command("sh", "-c", command);
                }

                pb.redirectErrorStream(true);
                process = pb.start();

                Thread readerThread = new Thread(this::readProcessOutput);
                readerThread.setDaemon(true);
                readerThread.start();

                int exitCode = process.waitFor();
                readerThread.join(1000);

                flushBuffer();
                appendOutput("\n>>> Exit code: " + exitCode + "\n\n");

                if (!running) break;
            }
            appendOutput(">>> All commands completed.\n");
            Platform.runLater(() -> tab.setText(entry.getName() + " " + Constants.STATUS_SUCCESS));
        } catch (Exception e) {
            appendOutput("\n>>> ERROR: " + e.getMessage() + "\n");
            Platform.runLater(() -> tab.setText(entry.getName() + " " + Constants.STATUS_ERROR));
        } finally {
            running = false;
        }
    }

    private void readProcessOutput() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            char[] buffer = new char[Constants.READER_BUFFER_SIZE];
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

            long now = System.currentTimeMillis();
            if (now - lastUIUpdate > Constants.UI_UPDATE_INTERVAL_MS ||
                    outputBuffer.length() > Constants.FLUSH_THRESHOLD) {
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

                    if (outputArea.getLength() > Constants.MAX_BUFFER_SIZE) {
                        outputArea.deleteText(0, outputArea.getLength() - Constants.MAX_BUFFER_SIZE);
                    }

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
            process.destroy();

            try {
                if (!process.waitFor(Constants.PROCESS_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }

            appendOutput("\n>>> Process terminated by user.\n");
            flushBuffer();
            Platform.runLater(() -> tab.setText(entry.getName() + " " + Constants.STATUS_TERMINATED));
        }
    }

    public boolean isRunning() {
        return running && process != null && process.isAlive();
    }

    public void setOutputArea(TextArea outputArea) {
        this.outputArea = outputArea;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }
}