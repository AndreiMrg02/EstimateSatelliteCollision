package com.ucv.Util;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class LoggerCustom {
    private static LoggerCustom instance;
    private TextFlow console;
    private ScrollPane scrollPane;

    private LoggerCustom() {
    }

    public static synchronized LoggerCustom getInstance() {
        if (instance == null) {
            instance = new LoggerCustom();
        }
        return instance;
    }

    public void setConsole(TextFlow console, ScrollPane scrollPane) {
        this.console = console;
        this.scrollPane = scrollPane;
        this.scrollPane.setContent(console);
    }

    public void logMessage(String message) {
        if (console != null) {
            Platform.runLater(() -> {
                Text text = new Text(message + "\n");
                if (message.contains("HIGH")) {
                    text.setFill(Color.RED);
                } else if (message.contains("MEDIUM")) {
                    text.setFill(Color.ORANGE);
                } else if (message.contains("LOW")) {
                    text.setFill(Color.GREEN);
                } else {
                    text.setFill(Color.BLACK);
                }
                console.getChildren().add(text);
                // Auto-scroll to the bottom
                scrollPane.layout();
                scrollPane.setVvalue(1.0);
            });
        }
    }
}
