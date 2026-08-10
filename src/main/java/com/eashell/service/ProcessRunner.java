package com.eashell.service;

import com.eashell.model.ScriptEntry;
import com.eashell.util.Constants;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class ProcessRunner implements Runnable {
    private final ScriptEntry entry;
    private final BiConsumer<String, Boolean> onStatusChange;
    private TextArea outputArea;
    private Tab tab;
    private Process process;
    private volatile BufferedWriter writer;
    private volatile boolean running = true;
    private final StringBuilder outputBuffer = new StringBuilder();
    private long lastUIUpdate = 0;
    private int pendingLineLength = 0;
    private int currentLineStartInBuffer = 0;
    private boolean discardPendingAreaLine = false;

    public ProcessRunner(ScriptEntry entry, TextArea outputArea, Tab tab,
                          BiConsumer<String, Boolean> onStatusChange) {
        this.entry = entry;
        this.outputArea = outputArea;
        this.tab = tab;
        this.onStatusChange = onStatusChange;
    }

    @Override
    public void run() {
        // Fired as the very first action so the card only shows "running" once a thread from
        // the bounded pool has actually picked this up - MainWindow marks it merely "queued"
        // at submit time, since with a full pool this run() call might not happen right away.
        onStatusChange.accept(entry.getId(), true);
        try {
            for (String command : entry.getCommands()) {
                if (!running) break;

                appendOutput(">>> Executing: " + command + "\n");

                ProcessBuilder pb = new ProcessBuilder();
                pb.directory(new File(entry.getWorkingDir()));

                if (System.getProperty("os.name").toLowerCase().contains("windows")) {

                    //  pb.command("cmd.exe", "/c", command);
                    String utf8Command = "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; " + command;
                    pb.command("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", utf8Command);
                } else {
                    pb.command("sh", "-c", command);
                }

                pb.redirectErrorStream(true);
                pb.environment().put("NO_COLOR", "1");
                pb.environment().put("TERM", "dumb");
                process = pb.start();
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), Constants.CONSOLE_CHARSET));

                Thread readerThread = new Thread(this::readProcessOutput);
                readerThread.setDaemon(true);
                readerThread.start();

                int exitCode = process.waitFor();
                readerThread.join(1000);
                closeWriter();

                flushBuffer();
                appendOutput("\n>>> Exit code: " + exitCode + "\n\n");

                if (!running) break;
            }
            // Only a natural finish gets the success title - if running is false here, the
            // loop broke early because of stop(), which already set its own tab title.
            if (running) {
                appendOutput(">>> All commands completed.\n");
                Platform.runLater(() -> tab.setText(entry.getName() + " " + Constants.STATUS_SUCCESS));
            }
        } catch (Exception e) {
            appendOutput("\n>>> ERROR: " + e.getMessage() + "\n");
            Platform.runLater(() -> tab.setText(entry.getName() + " " + Constants.STATUS_ERROR));
        } finally {
            running = false;
            closeWriter();
            onStatusChange.accept(entry.getId(), false);
        }
    }

    private void readProcessOutput() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Constants.CONSOLE_CHARSET))) {

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
            // Emulate a terminal's carriage-return-driven line overwrite: a bare \r rewinds
            // to the start of the current (not yet newline-terminated) line instead of
            // producing a new one, so progress bars collapse into a single updating line.
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\r') {
                    outputBuffer.setLength(currentLineStartInBuffer);
                    if (currentLineStartInBuffer == 0) {
                        discardPendingAreaLine = true;
                    }
                } else {
                    outputBuffer.append(c);
                    if (c == '\n') {
                        // Not resetting discardPendingAreaLine here: a \r earlier in this same
                        // chunk may have set it, and that request must survive until
                        // flushBuffer() consumes it. Once currentLineStartInBuffer moves past
                        // this \n, a later \r in the same buffer can't re-set the flag anyway
                        // (its "== 0" check will be false), so nothing needs clearing here.
                        currentLineStartInBuffer = outputBuffer.length();
                    }
                }
            }

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
                boolean discardPreviousLine = discardPendingAreaLine;
                int previousPendingLineLength = pendingLineLength;
                outputBuffer.setLength(0);
                currentLineStartInBuffer = 0;
                discardPendingAreaLine = false;

                int lastNewline = text.lastIndexOf('\n');
                int newTailLength = lastNewline == -1 ? text.length() : text.length() - lastNewline - 1;
                pendingLineLength = (discardPreviousLine || lastNewline != -1)
                        ? newTailLength
                        : previousPendingLineLength + newTailLength;

                Platform.runLater(() -> {
                    if (discardPreviousLine && previousPendingLineLength > 0) {
                        int len = outputArea.getLength();
                        outputArea.deleteText(Math.max(0, len - previousPendingLineLength), len);
                    }

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

    /**
     * Send a line of text to the running process's stdin (e.g. to answer a prompt).
     * Not every program reads stdin - full-screen TUIs and console-handle-based
     * prompts (like cmd's `pause`) ignore the pipe regardless.
     */
    public void sendInput(String line) {
        BufferedWriter currentWriter = writer;
        if (currentWriter == null) {
            appendOutput(">>> No running process to receive input.\n");
            return;
        }
        try {
            currentWriter.write(line);
            currentWriter.write(System.lineSeparator());
            currentWriter.flush(); // without flush the child just hangs waiting for input
            appendOutput(">>> " + line + "\n"); // local echo - the child does not echo stdin itself
        } catch (IOException e) {
            appendOutput("\n>>> Error sending input: " + e.getMessage() + "\n");
        }
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
            writer = null;
        }
    }

    /**
     * Stops the process. Every caller (STOP button, tab close, STOP ALL, window close) runs on
     * the FX thread, but destroying a stubborn process can block for up to
     * PROCESS_STOP_TIMEOUT_SECONDS - so the actual destroy-and-wait happens on a small daemon
     * thread instead of here, and this method returns immediately. `running = false` is still
     * set synchronously so the UI-facing effects of stopping start right away.
     */
    public void stop() {
        running = false;
        Process processToStop = process;
        if (processToStop != null && processToStop.isAlive()) {
            Thread stopThread = new Thread(() -> destroyAndWait(processToStop));
            stopThread.setDaemon(true);
            stopThread.start();
        }
    }

    private void destroyAndWait(Process processToStop) {
        // destroy() only kills the shell (powershell/sh); its children (node, yt-dlp,
        // dev servers, ...) would otherwise keep running and holding ports/files.
        processToStop.descendants().forEach(ProcessHandle::destroy);
        processToStop.destroy();

        try {
            if (!processToStop.waitFor(Constants.PROCESS_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                processToStop.descendants().forEach(ProcessHandle::destroyForcibly);
                processToStop.destroyForcibly();
            }
        } catch (InterruptedException e) {
            processToStop.descendants().forEach(ProcessHandle::destroyForcibly);
            processToStop.destroyForcibly();
            Thread.currentThread().interrupt();
        }

        appendOutput("\n>>> Process terminated by user.\n");
        flushBuffer();
        Platform.runLater(() -> tab.setText(entry.getName() + " " + Constants.STATUS_TERMINATED));
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