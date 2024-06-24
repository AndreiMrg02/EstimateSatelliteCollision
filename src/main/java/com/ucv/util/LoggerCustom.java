package com.ucv.util;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
        addContextMenu();
    }

    private void addContextMenu() {
        // Create "Clear" menu item
        MenuItem clearItem = new MenuItem("Clear");
        clearItem.setOnAction(e -> clearConsole());

        // Create "Copy" menu item
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> copyText());

        ContextMenu contextMenu = new ContextMenu(clearItem, copyItem);
        console.setOnContextMenuRequested(e ->
                contextMenu.show(console, e.getScreenX(), e.getScreenY()));
    }

    private void clearConsole() {
        Platform.runLater(() -> console.getChildren().clear());
    }

    private void copyText() {
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();
            console.getChildren().forEach(node -> {
                if (node instanceof Text) {
                    sb.append(((Text) node).getText());
                }
            });
            final ClipboardContent content = new ClipboardContent();
            content.putString(sb.toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
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
