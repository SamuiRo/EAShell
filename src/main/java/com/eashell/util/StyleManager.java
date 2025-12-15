package com.eashell.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class StyleManager {
    public static final String PRIMARY_BG = "#1a1425";
    public static final String SECONDARY_BG = "#241b2f";
    public static final String CARD_BG = "#2d2139";
    public static final String CARD_HOVER_BG = "#3a2847";
    public static final String ACCENT_PURPLE = "#9d7bd8";
    public static final String ACCENT_ELECTRIC = "#b8a4e8";
    public static final String ACCENT_GOLD = "#d4af37";
    public static final String ACCENT_GREEN = "#B794D4";
    public static final String ACCENT_BLUE = "transparent";
    public static final String ACCENT_PINK = "#e89fd9";
    public static final String ACCENT_RED = "#FF6B9D";
    public static final String BORDER_COLOR = "#4a3960";
    public static final String BORDER_GLOW = "#8b6bb8";
    public static final String TEXT_PRIMARY = "#e8dff5";
    public static final String TEXT_SECONDARY = "#b8a4e8";
    public static final String TEXT_MUTED = "#7a6b8f";
    public static final String TEXT_ACCENT = "#d4af37";

    public static final String BACKGROUND_TOP = "#1A0B2E";
    public static final String BACKGROUND_BOTTOM = "#2D1B3D";

    public static final String HEADER_BACKGROUND = "#2D1B3D";
    public static final String HEADER_TEXT = "#F5E6FF";
    public static final String HEADER_ASCENT = "#FFD68A";

    public static final String COMMAND_CARD_BACKGROUND = "#4A2C5E";
    public static final String COMMAND_CARD_BORDER = ACCENT_GREEN;
    public static final String COMMAND_CARD_HOVER = "#6B4A8C";
    public static final String COMMAND_CARD_SHADOW = "#7DD3E8";

    public static final String BUTTON_PRIMARY_TOP = ACCENT_GREEN;
    public static final String BUTTON_PRIMARY_BOTTOM = "#E291B5";
    public static final String BUTTON_PRIMARY_HOVER = "#7DD3E8";
    public static final String BUTTON_TEXT = "#FFFFFF";

    public static final String PRIMARY_BUTTON = "linear-gradient(to bottom, " + BUTTON_PRIMARY_TOP + ", " + BUTTON_PRIMARY_BOTTOM + ")";
    public static final String SECONDARY_BUTTON = "";
    public static final String DANGER_BUTTON = "linear-gradient(to bottom, " + ACCENT_RED + ", " + ACCENT_RED + ")";
    public static final String UTIL_BUTTON = "linear-gradient(to bottom, " + ACCENT_BLUE + ", " + ACCENT_BLUE + ")";

    // Fonts
    private static final String FONT_ELEGANT = "'Segoe UI', 'Helvetica Neue', sans-serif";
    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";

    // Main backgrounds with gradients
    public static String getRootStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + BACKGROUND_TOP + ", " + BACKGROUND_BOTTOM + ");";
    }

    public static String getSplitPaneStyle() {
        return "-fx-background-color: transparent;";
    }

    public static String getTopBarStyle() {
        return "-fx-background-color: rgba(45, 27, 61, 0.9);\n" +
                "-fx-padding: 20;\n" +
                "-fx-border-color: #B794D4;\n" +
                "-fx-border-width: 0 0 1 0;\n" +
                "-fx-effect: dropshadow(gaussian, rgba(125, 211, 232, 0.3), 10, 0, 0, 5);";
    }

    public static String getTitleStyle() {
        return "-fx-font-size: 28px;\n" +
                "-fx-font-weight: bold;\n" +
                "-fx-text-fill: linear-gradient(to right, #7DD3E8, #E291B5);\n" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(125, 211, 232, 0.5), 5, 0, 0, 0);";
    }

    public static String getStatusLabelStyle() {
        return "-fx-text-fill: " + HEADER_ASCENT + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 500;" +
                "-fx-font-family: " + FONT_ELEGANT + ";";
    }

    public static String getPanelStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + "#2D1B3D" + ", " + "#120e1a" + ");" +
                "-fx-background-radius: 12;";
    }

    public static String getOutputPanelStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + "#2D1B3D" + ", #120e1a);" +
                "-fx-background-radius: 12;";
    }

    public static String getHeaderStyle() {
        return "-fx-background-color: #2D1B3D;" +
                "-fx-text-fill: " + "#E291B5" + ";" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: 600;" +
                "-fx-font-family: " + FONT_ELEGANT + ";";
    }

    public static String getScrollPaneStyle() {
        return "-fx-background: transparent;" +
                "-fx-background-color: transparent;";
    }

    public static String getTabPaneStyle() {
        return "-fx-background-color: " + "#2D1B3D" + ";" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 0;;" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-radius: 2;" +
                "-fx-background-radius: 2;" +
                "-fx-background-color: #2D1B3D;\n" +
                "-fx-border-color: #4A3B5A;\n" +
                "-fx-border-radius: 2;\n" +
                "-fx-background-radius: 2;\n" +
                "-fx-background-insets: 0;\n" +
                "-fx-padding: 0;" +
                "-fx-tab-min-width: 100px;\n" +
                "-fx-tab-min-height: 30px;";
    }

    // Script card
    public static String getCardStyle() {
        return "-fx-background-color: #4A2C5E;\n" +
                "-fx-background-radius: 2;\n" +
                "-fx-border-color: #B794D4;\n" +
                "-fx-border-radius: 2;\n" +
                "-fx-border-width: 1;\n" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 8, 0, 0, 4);";
    }

    public static String getCardHoverStyle() {
        return "-fx-background-color: #6B4A8C;\n" +
                "-fx-border-color: #7DD3E8;\n" +
                "-fx-effect: dropshadow(gaussian, rgba(125, 211, 232, 0.5), 12, 0, 0, 6);\n" +
                "-fx-background-radius: 2;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 2;" +
                "-fx-cursor: hand;";
    }

    public static String getCardTitleStyle() {
        return "-fx-text-fill: " + "#F5E6FF" + ";" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: 600;" +
                "-fx-border-bottom: #6B4A8C;" +
                "-fx-border-width: 1;" +
                "-fx-font-family: " + FONT_ELEGANT + ";";
    }

    public static String getCardPathStyle() {
        return "-fx-text-fill: " + TEXT_MUTED + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-style: italic;" +
                "-fx-font-family: " + FONT_ELEGANT + ";";
    }

    public static String getCardCommandStyle() {
        return "-fx-text-fill: " + TEXT_SECONDARY + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-family: " + FONT_MONO + ";";
    }

    public static String getEmptyLabelStyle() {
        return "-fx-text-fill: " + TEXT_MUTED + ";" +
                "-fx-font-size: 14px;" +
                "-fx-text-alignment: center;" +
                "-fx-font-family: " + FONT_ELEGANT + ";";
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
        String baseStyle = getSmallButtonStyle(color);
        String hoverStyle = getSmallButtonHoverStyle(color);

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        return btn;
    }

    private static String getButtonStyle(String style) {
        return "-fx-background-color: " + style + ";" +
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 600;" +
                "-fx-background-radius: 2;" +
                "-fx-padding: 10 20 10 20;" +
                "-fx-cursor: hand;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(226, 145, 181, 0.4), 6, 0, 0, 3);";
    }

    private static String getButtonHoverStyle(String style) {
        return "-fx-background-color: linear-gradient(to bottom, #E291B5, #B794D4);" +
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 600;" +
                "-fx-background-radius: 2;" +
                "-fx-padding: 10 20 10 20;" +
                "-fx-cursor: hand;" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(125, 211, 232, 0.6), 10, 0, 0, 5);";
    }

    private static String getSmallButtonStyle(String style) {
        return "-fx-background-color: " + style + ";" +
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 2;" +
                "-fx-border-radius: 2;" +
                "-fx-border-color: #B794D4;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 4, 0.4, 0, 1);";
    }

    private static String getSmallButtonHoverStyle(String color) {
        return "-fx-background-color: derive(" + "#7DD3E8" + ", 20%);" +
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 2;" +
                "-fx-border-radius: 2;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(157, 123, 216, 0.5), 6, 0.6, 0, 1);";
    }

    // Form elements
    public static Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + TEXT_SECONDARY + ";" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-font-weight: 500;");
        return lbl;
    }

    public static void styleTextField(TextField field) {
        field.setStyle("-fx-background-color: " + PRIMARY_BG + ";" +
                "-fx-text-fill: " + "#7DD3E8" + ";" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-radius: 2;" +
                "-fx-background-radius: 2;" +
                "-fx-padding: 8;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-prompt-text-fill: " + TEXT_MUTED + ";");

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle("-fx-background-color: " + PRIMARY_BG + ";" +
                        "-fx-text-fill: " + "#7DD3E8" + ";" +
                        "-fx-border-color: " + BORDER_GLOW + ";" +
                        "-fx-border-radius: 2;" +
                        "-fx-background-radius: 2;" +
                        "-fx-padding: 8;" +
                        "-fx-font-family: " + FONT_ELEGANT + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(157, 123, 216, 0.4), 8, 0.5, 0, 0);");
            } else {
                styleTextField(field);
            }
        });
    }

    public static void styleTextArea(TextArea area) {
        area.setStyle("-fx-control-inner-background: " + PRIMARY_BG + ";" +
                "-fx-text-fill: " + "#7DD3E8" + ";" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-radius: 2;" +
                "-fx-background-radius: 2;" +
                "-fx-font-family: " + FONT_MONO + ";" +
                "-fx-prompt-text-fill: " + TEXT_MUTED + ";");

        area.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                area.setStyle("-fx-control-inner-background: " + PRIMARY_BG + ";" +
                        "-fx-text-fill: " + "#7DD3E8" + ";" +
                        "-fx-border-color: " + BORDER_GLOW + ";" +
                        "-fx-border-radius: 2;" +
                        "-fx-background-radius: 2;" +
                        "-fx-font-family: " + FONT_MONO + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(157, 123, 216, 0.4), 8, 0.5, 0, 0);");
            } else {
                styleTextArea(area);
            }
        });
    }

    public static String getOutputAreaStyle() {
        return "-fx-control-inner-background: #0d0a12;" +
                "-fx-background-color: #0d0a12;" +
                "-fx-text-fill: " + ACCENT_ELECTRIC + ";" +
                "-fx-text-fill: " + "#7DD3E8" + ";" +
                "-fx-font-family: " + FONT_MONO + ";" +
                "-fx-font-size: 12px;" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-radius: 2;" +
                "-fx-background-radius: 2;";
    }// FIXME text fill choose one

    public static String getDialogStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + CARD_BG + ", " + PRIMARY_BG + ");" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + BORDER_GLOW + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;";
    }

    public static String getTabContentStyle() {
        return "-fx-background-color: " + PRIMARY_BG + ";" +
                "-fx-background-radius: 8;";
    }

    // Status indicators
    public static void setRunningStatus(Label label) {
        label.setText(Constants.STATUS_RUNNING);
        label.setStyle("-fx-text-fill: " + HEADER_ASCENT + ";" );
    }

    public static void setStoppedStatus(Label label) {
        label.setText(Constants.STATUS_STOPPED);
        label.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
    }

    public static String getStylesheet() {
        return "data:text/css," +
                // Scroll bars
                ".scroll-pane { -fx-background-color: transparent; }" +
                ".scroll-pane .viewport { -fx-background-color: transparent; }" +
                ".scroll-pane .content { -fx-background-color: transparent; }" +
                ".scroll-bar { -fx-background-color: transparent; }" +
                ".scroll-bar .thumb { " +
                "-fx-background-color: " + BORDER_COLOR + ";" +
                "-fx-background-radius: 6; }" +
                ".scroll-bar .thumb:hover { " +
                "-fx-background-color: " + BORDER_GLOW + "; }" +

                // === critical for tabs ===
                ".tab-pane { " +
                "-fx-background-color: #2D1B3D; }" +

                ".tab-pane .tab-header-area { " +
                "-fx-background-color: #2D1B3D; " +
                "-fx-padding: 0 0 0 5px; }" +

                // remove white bg
                ".tab-pane .tab-header-background { " +
                "-fx-background-color: #2D1B3D; }" +

                ".tab-pane .tab-content-area { " +
                "-fx-background-color: #2D1B3D; }" +

                // Tabs
                ".tab-pane .tab { " +
                "-fx-background-color: #3A294A;" +
                "-fx-border-color: #4A3B5A;" +
                "-fx-background-radius: 5 5 0 0;" +
                "-fx-padding: 5 10; }" +

                ".tab-pane .tab:selected { " +
                "-fx-background-color: #4A3B5A; }" +

                ".tab-pane .tab-label { " +
                "-fx-text-fill: " + TEXT_SECONDARY + "; }" +

                // Separator
                ".separator { " +
                "-fx-background-color: " + BORDER_COLOR + "; }" +

                // TextArea
                ".text-area { " +
                "-fx-background-color: #0d0a12; }" +
                ".text-area .content { " +
                "-fx-background-color: #0d0a12; }" +
                ".text-area:focused .content { " +
                "-fx-background-color: #0d0a12; }" +
                ".text-area:window-unfocused .content { " +
                "-fx-background-color: #0d0a12 !important; }" +
                ".text-area:window-unfocused { " +
                "-fx-background-color: #0d0a12 !important; }" +

                // SplitPane
                ".split-pane { " +
                "-fx-background-color: #000000; " +
                "-fx-border-color: #000000; " +
                "-fx-border-width: 1; }" +

                ".split-pane > .split-pane-divider { " +
                "-fx-background-color: #4A3B5A; " +
                "-fx-background-insets: 0; " +
                "-fx-padding: 0 3 0 3; " +
                "-fx-border-color: transparent; }" +

                ".split-pane > .split-pane-divider:hover { " +
                "-fx-background-color: #7A5B9A; }";
    }
}