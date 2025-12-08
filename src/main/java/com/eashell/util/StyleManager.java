package com.eashell.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class StyleManager {
    // Keqing-inspired Colors
    public static final String PRIMARY_BG = "#1a1425";
    public static final String SECONDARY_BG = "#241b2f";
    public static final String CARD_BG = "#2d2139";
    public static final String CARD_HOVER_BG = "#3a2847";
    public static final String ACCENT_PURPLE = "#9d7bd8";
    public static final String ACCENT_ELECTRIC = "#b8a4e8";
    public static final String ACCENT_GOLD = "#d4af37";
    public static final String ACCENT_GREEN = ACCENT_ELECTRIC;  // Purple/Electro замість зеленого
    public static final String ACCENT_BLUE = ACCENT_PURPLE;
    public static final String ACCENT_PINK = "#e89fd9";
    public static final String ACCENT_RED = "#e85d75";
    public static final String BORDER_COLOR = "#4a3960";
    public static final String BORDER_GLOW = "#8b6bb8";
    public static final String TEXT_PRIMARY = "#e8dff5";
    public static final String TEXT_SECONDARY = "#b8a4e8";
    public static final String TEXT_MUTED = "#7a6b8f";
    public static final String TEXT_ACCENT = "#d4af37";

    // Fonts
    private static final String FONT_ELEGANT = "'Segoe UI', 'Helvetica Neue', sans-serif";
    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";

    // Main backgrounds with gradients
    public static String getRootStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + PRIMARY_BG + ", #16111d);";
    }

    public static String getSplitPaneStyle() {
        return "-fx-background-color: transparent;";
    }

    // Top bar with elegant gradient
    public static String getTopBarStyle() {
        return "-fx-background-color: linear-gradient(to right, " + CARD_BG + ", #3d2952);" +
                "-fx-border-color: " + BORDER_GLOW + ";" +
                "-fx-border-width: 0 0 2 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(157, 123, 216, 0.4), 10, 0.5, 0, 2);";
    }

    public static String getTitleStyle() {
        return "-fx-text-fill: linear-gradient(to right, " + ACCENT_ELECTRIC + ", " + ACCENT_PINK + ");" +
                "-fx-font-size: 26px;" +
                "-fx-font-weight: 600;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(184, 164, 232, 0.6), 8, 0.7, 0, 0);";
    }

    public static String getStatusLabelStyle() {
        return "-fx-text-fill: " + TEXT_SECONDARY + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 500;" +
                "-fx-font-family: " + FONT_ELEGANT + ";";
    }

    // Panels with subtle gradients
    public static String getPanelStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + SECONDARY_BG + ", " + PRIMARY_BG + ");" +
                "-fx-background-radius: 12;";
    }

    public static String getOutputPanelStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + PRIMARY_BG + ", #120e1a);" +
                "-fx-background-radius: 12;";
    }

    public static String getHeaderStyle() {
        return "-fx-text-fill: " + ACCENT_ELECTRIC + ";" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: 600;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(184, 164, 232, 0.5), 6, 0.6, 0, 0);";
    }

    public static String getScrollPaneStyle() {
        return "-fx-background: transparent;" +
                "-fx-background-color: transparent;";
    }

    public static String getTabPaneStyle() {
        return "-fx-background-color: " + PRIMARY_BG + ";" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;";
    }

    // Script card with elegant styling
    public static String getCardStyle() {
        return "-fx-background-color: linear-gradient(to bottom right, " + CARD_BG + ", #261c33);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 8, 0.3, 0, 2);";
    }

    public static String getCardHoverStyle() {
        return "-fx-background-color: linear-gradient(to bottom right, " + CARD_HOVER_BG + ", #32273f);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + BORDER_GLOW + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(157, 123, 216, 0.6), 12, 0.6, 0, 2);";
    }

    public static String getCardTitleStyle() {
        return "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: 600;" +
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

    // Buttons with elegant gradients
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

    private static String getButtonStyle(String color) {
        return "-fx-background-color: linear-gradient(to bottom, " + color + ", derive(" + color + ", -15%));" +
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-font-weight: 600;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 6, 0.5, 0, 2);";
    }

    private static String getButtonHoverStyle(String color) {
        return "-fx-background-color: linear-gradient(to bottom, derive(" + color + ", 20%), " + color + ");" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-weight: 600;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(157, 123, 216, 0.6), 10, 0.7, 0, 2);" +
                "-fx-scale-y: 1.05;" +
                "-fx-scale-x: 1.05;";
    }

    private static String getSmallButtonStyle(String color) {
        return "-fx-background-color: " + color + ";" +
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 6;" +
                "-fx-border-radius: 6;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 4, 0.4, 0, 1);";
    }

    private static String getSmallButtonHoverStyle(String color) {
        return "-fx-background-color: derive(" + color + ", 20%);" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 6;" +
                "-fx-border-radius: 6;" +
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
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 8;" +
                "-fx-font-family: " + FONT_ELEGANT + ";" +
                "-fx-prompt-text-fill: " + TEXT_MUTED + ";");

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle("-fx-background-color: " + PRIMARY_BG + ";" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-border-color: " + BORDER_GLOW + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
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
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-font-family: " + FONT_MONO + ";" +
                "-fx-prompt-text-fill: " + TEXT_MUTED + ";");

        area.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                area.setStyle("-fx-control-inner-background: " + PRIMARY_BG + ";" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-border-color: " + BORDER_GLOW + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-family: " + FONT_MONO + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(157, 123, 216, 0.4), 8, 0.5, 0, 0);");
            } else {
                styleTextArea(area);
            }
        });
    }

    public static String getOutputAreaStyle() {
        return "-fx-control-inner-background: #0d0a12;" +
                "-fx-text-fill: " + ACCENT_ELECTRIC + ";" +
                "-fx-font-family: " + FONT_MONO + ";" +
                "-fx-font-size: 12px;" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;";
    }

    public static String getDialogStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + CARD_BG + ", " + PRIMARY_BG + ");" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + BORDER_GLOW + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;";
    }

    public static String getTabContentStyle() {
        return "-fx-background-color: " + PRIMARY_BG + ";" +
                "-fx-background-radius: 8;";
    }

    // Status indicators with glow effects
    public static void setRunningStatus(Label label) {
        label.setText(Constants.STATUS_RUNNING);
        label.setStyle("-fx-text-fill: " + ACCENT_ELECTRIC + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(184, 164, 232, 0.8), 10, 0.8, 0, 0);");
    }

    public static void setStoppedStatus(Label label) {
        label.setText(Constants.STATUS_STOPPED);
        label.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
    }

    // CSS Stylesheet with enhanced styling
    public static String getStylesheet() {
        return "data:text/css," +
                ".scroll-pane { -fx-background-color: transparent; }" +
                ".scroll-pane .viewport { -fx-background-color: transparent; }" +
                ".scroll-pane .content { -fx-background-color: transparent; }" +
                ".scroll-bar { -fx-background-color: transparent; }" +
                ".scroll-bar .thumb { " +
                "-fx-background-color: " + BORDER_COLOR + ";" +
                "-fx-background-radius: 6; }" +
                ".scroll-bar .thumb:hover { " +
                "-fx-background-color: " + BORDER_GLOW + "; }" +
                ".tab-pane .tab { " +
                "-fx-background-color: " + CARD_BG + ";" +
                "-fx-border-color: " + BORDER_COLOR + ";" +
                "-fx-background-radius: 8 8 0 0; }" +
                ".tab-pane .tab:selected { " +
                "-fx-background-color: " + CARD_HOVER_BG + ";" +
                "-fx-border-color: " + BORDER_GLOW + "; }" +
                ".tab-pane .tab-label { " +
                "-fx-text-fill: " + TEXT_SECONDARY + "; }" +
                ".separator { " +
                "-fx-background-color: " + BORDER_COLOR + "; }";
    }
}