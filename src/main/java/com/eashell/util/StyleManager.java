package com.eashell.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class StyleManager {
    // Colors
    public static final String PRIMARY_BG = "#0a0e27";
    public static final String SECONDARY_BG = "#0f1729";
    public static final String CARD_BG = "#1a1f3a";
    public static final String CARD_HOVER_BG = "#232a4a";
    public static final String ACCENT_GREEN = "#00ff41";
    public static final String ACCENT_BLUE = "#5af";
    public static final String ACCENT_RED = "#ff4444";
    public static final String BORDER_COLOR = "#2a3f5f";
    public static final String TEXT_PRIMARY = "#00ff41";
    public static final String TEXT_SECONDARY = "#aaa";
    public static final String TEXT_MUTED = "#666";

    // Fonts
    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";

    // Main backgrounds
    public static String getRootStyle() {
        return "-fx-background-color: " + PRIMARY_BG + ";";
    }

    public static String getSplitPaneStyle() {
        return "-fx-background-color: " + PRIMARY_BG + ";";
    }

    // Top bar
    public static String getTopBarStyle() {
        return "-fx-background-color: linear-gradient(to right, #1a1f3a, #0f1729); " +
                "-fx-border-color: " + ACCENT_GREEN + "; -fx-border-width: 0 0 2 0;";
    }

    public static String getTitleStyle() {
        return "-fx-text-fill: " + ACCENT_GREEN + "; -fx-font-size: 24px; " +
                "-fx-font-weight: bold; -fx-font-family: " + FONT_MONO + ";";
    }

    public static String getStatusLabelStyle() {
        return "-fx-text-fill: " + ACCENT_BLUE + "; -fx-font-size: 14px; " +
                "-fx-font-family: " + FONT_MONO + ";";
    }

    // Panels
    public static String getPanelStyle() {
        return "-fx-background-color: " + SECONDARY_BG + ";";
    }

    public static String getOutputPanelStyle() {
        return "-fx-background-color: " + PRIMARY_BG + ";";
    }

    public static String getHeaderStyle() {
        return "-fx-text-fill: " + ACCENT_GREEN + "; -fx-font-size: 18px; " +
                "-fx-font-weight: bold; -fx-font-family: " + FONT_MONO + ";";
    }

    public static String getScrollPaneStyle() {
        return "-fx-background: " + SECONDARY_BG + "; -fx-background-color: " + SECONDARY_BG + ";";
    }

    public static String getTabPaneStyle() {
        return "-fx-background-color: " + SECONDARY_BG + ";";
    }

    // Script card
    public static String getCardStyle() {
        return "-fx-background-color: " + CARD_BG + "; -fx-background-radius: 8; " +
                "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1; -fx-border-radius: 8;";
    }

    public static String getCardHoverStyle() {
        return "-fx-background-color: " + CARD_HOVER_BG + "; -fx-background-radius: 8; " +
                "-fx-border-color: " + ACCENT_GREEN + "; -fx-border-width: 1; -fx-border-radius: 8;";
    }

    public static String getCardTitleStyle() {
        return "-fx-text-fill: " + ACCENT_GREEN + "; -fx-font-size: 16px; " +
                "-fx-font-weight: bold; -fx-font-family: " + FONT_MONO + ";";
    }

    public static String getCardPathStyle() {
        return "-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 11px;";
    }

    public static String getCardCommandStyle() {
        return "-fx-text-fill: " + ACCENT_BLUE + "; -fx-font-size: 12px; " +
                "-fx-font-family: " + FONT_MONO + ";";
    }

    public static String getEmptyLabelStyle() {
        return "-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 14px; -fx-text-alignment: center;";
    }

    // Buttons
    public static Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        String baseStyle = getButtonStyle(color);
        String hoverStyle = getButtonHoverStyle(color);

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        return btn;
    }

    public static Button createSmallButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #000; " +
                "-fx-font-size: 10px; -fx-padding: 5 10; -fx-background-radius: 3; " +
                "-fx-font-family: " + FONT_MONO + ";");
        return btn;
    }

    private static String getButtonStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-text-fill: #000; " +
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; " +
                "-fx-background-radius: 5; -fx-font-family: " + FONT_MONO + ";";
    }

    private static String getButtonHoverStyle(String color) {
        return "-fx-background-color: derive(" + color + ", 20%); -fx-text-fill: #000; " +
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; " +
                "-fx-background-radius: 5; -fx-font-family: " + FONT_MONO + ";";
    }

    // Form elements
    public static Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + ACCENT_GREEN + "; -fx-font-family: " + FONT_MONO + ";");
        return lbl;
    }

    public static void styleTextField(TextField field) {
        field.setStyle("-fx-background-color: " + PRIMARY_BG + "; -fx-text-fill: #0f0; " +
                "-fx-border-color: " + ACCENT_GREEN + "; -fx-border-radius: 3; " +
                "-fx-font-family: " + FONT_MONO + ";");
    }

    public static void styleTextArea(TextArea area) {
        area.setStyle("-fx-control-inner-background: " + PRIMARY_BG + "; -fx-text-fill: #0f0; " +
                "-fx-border-color: " + ACCENT_GREEN + "; -fx-font-family: " + FONT_MONO + ";");
    }

    public static String getOutputAreaStyle() {
        return "-fx-control-inner-background: #000; -fx-text-fill: #0f0; " +
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12px;";
    }

    public static String getDialogStyle() {
        return "-fx-background-color: " + CARD_BG + ";";
    }

    public static String getTabContentStyle() {
        return "-fx-background-color: " + PRIMARY_BG + ";";
    }

    // Status indicators
    public static void setRunningStatus(Label label) {
        label.setText(Constants.STATUS_RUNNING);
        label.setStyle("-fx-text-fill: #0f0;");
    }

    public static void setStoppedStatus(Label label) {
        label.setText(Constants.STATUS_STOPPED);
        label.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
    }

    // CSS Stylesheet
    public static String getStylesheet() {
        return "data:text/css," +
                ".scroll-pane { -fx-background-color: transparent; }" +
                ".scroll-pane .viewport { -fx-background-color: transparent; }" +
                ".scroll-pane .content { -fx-background-color: transparent; }";
    }
}