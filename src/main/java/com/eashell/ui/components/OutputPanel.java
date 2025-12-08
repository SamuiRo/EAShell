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

public class OutputPanel extends VBox {
    private final TabPane outputTabPane;

    public OutputPanel() {
        setSpacing(10);
        setPadding(new Insets(20));
        setStyle(StyleManager.getOutputPanelStyle());

        Label header = new Label(Constants.OUTPUT_HEADER);
        header.setStyle(StyleManager.getHeaderStyle());

        outputTabPane = new TabPane();
        outputTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        outputTabPane.setStyle(StyleManager.getTabPaneStyle());

        VBox.setVgrow(outputTabPane, Priority.ALWAYS);
        getChildren().addAll(header, new Separator(), outputTabPane);
    }

    public Tab createOutputTab(ScriptEntry entry,
                               ProcessRunner runner,
                               BiConsumer<String, Boolean> onStatusChange) {
        Tab outputTab = new Tab(entry.getName() + " " + Constants.STATUS_RUNNING);
        outputTab.setClosable(true);

        VBox tabContent = new VBox(5);
        tabContent.setPadding(new Insets(10));
        tabContent.setStyle(StyleManager.getTabContentStyle());

        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle(StyleManager.getOutputAreaStyle());
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        // Передаємо runner в createControlBox
        HBox controlBox = createControlBox(runner, outputArea);

        tabContent.getChildren().addAll(outputArea, controlBox);
        outputTab.setContent(tabContent);

        // Зберігаємо runner в userData Tab для доступу при закритті
        outputTab.setUserData(runner);

        outputTab.setOnClosed(e -> {
            // Отримуємо runner з userData
            ProcessRunner tabRunner = (ProcessRunner) outputTab.getUserData();
            if (tabRunner != null) {
                tabRunner.stop();
            }
            onStatusChange.accept(entry.getName(), false);
        });

        outputTabPane.getTabs().add(outputTab);
        outputTabPane.getSelectionModel().select(outputTab);

        return outputTab;
    }

    private HBox createControlBox(ProcessRunner runner, TextArea outputArea) {
        HBox controlBox = new HBox(8);

        Button stopBtn = StyleManager.createSmallButton("⏹ STOP", StyleManager.ACCENT_RED);
        stopBtn.setOnAction(e -> {
            // Перевіряємо чи runner не null перед викликом stop()
            if (runner != null) {
                runner.stop();
            }
        });

        Button clearBtn = StyleManager.createSmallButton("🗑 CLEAR", StyleManager.ACCENT_BLUE);
        clearBtn.setOnAction(e -> outputArea.clear());

        controlBox.getChildren().addAll(stopBtn, clearBtn);
        return controlBox;
    }

    public TextArea getOutputAreaFromTab(Tab tab) {
        VBox content = (VBox) tab.getContent();
        return (TextArea) content.getChildren().get(0);
    }
}