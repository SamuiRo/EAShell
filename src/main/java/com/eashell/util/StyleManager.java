package com.eashell.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Style-class-name constants and factory helpers for the app's look.
 *
 * All actual colors and CSS rules live in {@code /styles/app.css} (loaded once by
 * MainWindow), not here - this class only picks WHICH style class(es) a node gets. Palette
 * colors are custom properties on {@code .root} in that file, referenced by every rule that
 * needs them, so a color exists in exactly one place.
 */
public class StyleManager {
    // Button roles, named by what they mean rather than what hue they happen to be today.
    public static final String BTN_PRIMARY = "btn-primary"; // gradient - the one prominent action (+ NEW SCRIPT)
    public static final String BTN_ACCENT = "btn-accent";   // RUN / RUN GROUP
    public static final String BTN_NEUTRAL = "btn-neutral"; // EDIT / Browse / CLEAR
    public static final String BTN_DANGER = "btn-danger";   // DELETE / STOP / STOP ALL

    public static Button createStyledButton(String text, String roleStyleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn-large", roleStyleClass);
        return btn;
    }

    public static Button createSmallButton(String text, String roleStyleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn-small", roleStyleClass);
        return btn;
    }

    public static Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("form-label");
        return lbl;
    }

    // Status indicators (⚫/🟡/🟢)
    private static final String STATUS_RUNNING_CLASS = "status-running";
    private static final String STATUS_QUEUED_CLASS = "status-queued";
    private static final String STATUS_STOPPED_CLASS = "status-stopped";

    public static void setRunningStatus(Label label) {
        label.setText(Constants.STATUS_RUNNING);
        label.getStyleClass().removeAll(STATUS_RUNNING_CLASS, STATUS_QUEUED_CLASS, STATUS_STOPPED_CLASS);
        label.getStyleClass().add(STATUS_RUNNING_CLASS);
    }

    public static void setQueuedStatus(Label label) {
        label.setText(Constants.STATUS_QUEUED);
        label.getStyleClass().removeAll(STATUS_RUNNING_CLASS, STATUS_QUEUED_CLASS, STATUS_STOPPED_CLASS);
        label.getStyleClass().add(STATUS_QUEUED_CLASS);
    }

    public static void setStoppedStatus(Label label) {
        label.setText(Constants.STATUS_STOPPED);
        label.getStyleClass().removeAll(STATUS_RUNNING_CLASS, STATUS_QUEUED_CLASS, STATUS_STOPPED_CLASS);
        label.getStyleClass().add(STATUS_STOPPED_CLASS);
    }

    private StyleManager() {} // Prevent instantiation
}
