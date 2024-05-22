package com.ucv.controller;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.OutputStream;

public class ConsoleOutputStream extends OutputStream {
    private TextArea textArea;

    public ConsoleOutputStream(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) {
        Platform.runLater(() -> textArea.appendText(String.valueOf((char) b)));
    }
}
