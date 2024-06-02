package com.ucv.util;

import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

public class PaneCustomStyle {

    public void addClip(Region region, double arcWidth, double arcHeight) {
        region.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            Rectangle clip = new Rectangle(newValue.getWidth(), newValue.getHeight());
            clip.setArcWidth(arcWidth);
            clip.setArcHeight(arcHeight);
            region.setClip(clip);
        });
    }

    public void addClip(Region region, double width, double height, double arcWidth, double arcHeight) {
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(arcWidth);
        clip.setArcHeight(arcHeight);
        region.setClip(clip);
    }
}
