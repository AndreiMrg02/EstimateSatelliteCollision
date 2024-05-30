package com.ucv.Util;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class LoggerCustom {
    private static LoggerCustom instance;
    private TextArea console;

    private LoggerCustom() {
    }

    public static synchronized LoggerCustom getInstance() {
        if (instance == null) {
            instance = new LoggerCustom();
        }
        return instance;
    }

    public void setConsole(TextArea console) {
        this.console = console;
    }

    public void logMessage(String message) {
        if (console != null) { // Afișează doar mesajele de eroare
            Platform.runLater(() -> console.appendText(message + "\n"));
        }
    }
}
