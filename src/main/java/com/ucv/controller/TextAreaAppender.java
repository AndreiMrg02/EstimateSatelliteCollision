package com.ucv.controller;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class TextAreaAppender extends AppenderSkeleton {
    private TextArea textArea;

    public TextAreaAppender(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    protected void append(LoggingEvent event) {
        final String message = this.layout.format(event);
        Platform.runLater(() -> textArea.appendText(message));
    }

    @Override
    public void close() {
        // Implementare în cazul în care trebuie să eliberezi resurse
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }
}
