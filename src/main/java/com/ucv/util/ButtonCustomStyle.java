package com.ucv.util;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Button;
import javafx.util.Duration;

public class ButtonCustomStyle {
    public void setButtonStyle(Button button) {
        button.setOnMouseEntered(e -> {
            button.getStyleClass().add("buttonBackgroundHovered");
            ScaleTransition st = new ScaleTransition(Duration.millis(500), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), button);
            tt.setToY(-10);
            tt.play();
        });

        button.setOnMouseExited(e -> {
            button.getStyleClass().remove("buttonBackgroundHovered");
            ScaleTransition st = new ScaleTransition(Duration.millis(500), button);
            st.setToX(1);
            st.setToY(1);
            st.play();
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), button);
            tt.setToY(0);
            tt.play();
        });
    }
}
