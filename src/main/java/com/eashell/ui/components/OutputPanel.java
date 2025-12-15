package com.eashell.ui.components;

import com.eashell.model.ScriptEntry;
import com.eashell.service.ProcessRunner;
import com.eashell.util.Constants;
import com.eashell.util.StyleManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;

/**
 * OUTPUT PANEL (RIGHT SIDE OF WINDOW)
 *
 * Responsible for displaying console output of running scripts.
 * Each script opens in a separate tab.
 *
 * Structure:
 * ┌─────────────────────────────────┐
 * │ 📟 CONSOLE          (header)    │
 * ├─────────────────────────────────┤
 * │ [Tab1] [Tab2] [Tab3]  (tabs)    │
 * │ ┌─────────────────────────────┐ │
 * │ │                             │ │
 * │ │  Console output here        │ │ <- TextArea (outputArea)
 * │ │  (execution text)           │ │
 * │ │                             │ │
 * │ └─────────────────────────────┘ │
 * │ [⏹ STOP] [🗑 CLEAR]  (buttons)  │
 * └─────────────────────────────────┘
 */
public class OutputPanel extends VBox {
    // Container for all tabs with output from different scripts
    private final TabPane outputTabPane;

    public OutputPanel() {
        // Spacing between panel elements
        setSpacing(10);
        setPadding(new Insets(20));

        // Apply dark gradient background to panel
        setStyle(StyleManager.getOutputPanelStyle());

        // === HEADER "📟 CONSOLE" ===
        Label header = new Label(Constants.OUTPUT_HEADER); // "📟 CONSOLE"
        header.setStyle(StyleManager.getHeaderStyle()); // Purple glowing text

        // === TAB CONTAINER ===
        outputTabPane = new TabPane();
        // Allow closing tabs (X button on each tab)
        outputTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        outputTabPane.setStyle(StyleManager.getTabPaneStyle());

        // TabPane stretches to full available height
        VBox.setVgrow(outputTabPane, Priority.ALWAYS);

        // Add all elements: header, separator, tab container
        getChildren().addAll(header, new Separator(), outputTabPane);
    }

    /**
     * CREATE NEW TAB FOR SCRIPT
     *
     * Called when user clicks "▶ RUN" on a script card.
     * Creates a new tab with console output and control buttons.
     *
     * @param entry - script information (name, commands, working directory)
     * @param runner - object that executes commands and captures output
     * @param onStatusChange - callback to update status (🟢/⚫) on script card
     */
    public Tab createOutputTab(ScriptEntry entry,
                               ProcessRunner runner,
                               BiConsumer<String, Boolean> onStatusChange) {

        // === CREATE TAB ===
        // Tab name: "Script Name 🟢" (with running indicator)
        Tab outputTab = new Tab(entry.getName() + " " + Constants.STATUS_RUNNING);
        outputTab.setClosable(true); // Can be closed with X button

        // === TAB CONTENT (vertical container) ===
        VBox tabContent = new VBox(5); // 5px between elements
        tabContent.setPadding(new Insets(10));
        tabContent.setStyle(StyleManager.getTabContentStyle()); // Dark background

        // === TEXT AREA FOR CONSOLE OUTPUT ===
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false); // Read-only
        outputArea.setWrapText(true); // Wrap long lines
        outputArea.setStyle(StyleManager.getOutputAreaStyle()); // Monospace font, dark background

        // TextArea stretches to full available tab height
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        // === CONTROL BUTTON PANEL ===
        // Pass runner so STOP button can stop the process
        HBox controlBox = createControlBox(runner, outputArea);

        // Add text area and buttons to tab content
        tabContent.getChildren().addAll(outputArea, controlBox);
        outputTab.setContent(tabContent);

        // === SAVE RUNNER IN TAB ===
        // Needed to stop process when tab is closed
        outputTab.setUserData(runner);

        // === TAB CLOSE HANDLER ===
        outputTab.setOnClosed(e -> {
            // Get runner from userData
            ProcessRunner tabRunner = (ProcessRunner) outputTab.getUserData();
            if (tabRunner != null) {
                tabRunner.stop(); // Stop process
            }
            // Update status on script card (⚫ - stopped)
            onStatusChange.accept(entry.getName(), false);
        });

        // Add new tab to TabPane and make it active
        outputTabPane.getTabs().add(outputTab);
        outputTabPane.getSelectionModel().select(outputTab);

        return outputTab;
    }

    /**
     * CONTROL BUTTON PANEL
     *
     * Creates a horizontal row of buttons below the text area:
     * [⏹ STOP] - stop script execution
     * [🗑 CLEAR] - clear console output
     */
    private HBox createControlBox(ProcessRunner runner, TextArea outputArea) {
        HBox controlBox = new HBox(8); // 8px between buttons

        // === STOP BUTTON ===
        Button stopBtn = StyleManager.createSmallButton("⏹ STOP", StyleManager.DANGER_BUTTON);
        stopBtn.setOnAction(e -> {
            // Check that runner is not null before calling stop()
            if (runner != null) {
                runner.stop(); // Stop process
            }
        });

        // === CLEAR BUTTON ===
        Button clearBtn = StyleManager.createSmallButton("🗑 CLEAR", StyleManager.UTIL_BUTTON);
        clearBtn.setOnAction(e -> outputArea.clear()); // Clear text in TextArea

        controlBox.getChildren().addAll(stopBtn, clearBtn);
        return controlBox;
    }

    /**
     * GET TEXT AREA FROM TAB
     *
     * Used to add text to console output.
     * ProcessRunner calls this method to write command output.
     */
    public TextArea getOutputAreaFromTab(Tab tab) {
        VBox content = (VBox) tab.getContent(); // Get VBox from tab
        return (TextArea) content.getChildren().get(0); // TextArea is first element
    }
}