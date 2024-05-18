package com.ucv.Util;

import javafx.scene.control.Tooltip;
import javafx.stage.Window;

public class ToolTipController {
    private static Tooltip tooltip = new Tooltip();

    static {
        tooltip.setAutoHide(true);
    }

    public static void showToolTip(javafx.geometry.Point2D screenLocation, String text) {
        tooltip.setText(text);
        tooltip.show(Window.getWindows().get(0), screenLocation.getX(), screenLocation.getY());
    }

    public static void hideToolTip() {
        tooltip.hide();
    }
}
