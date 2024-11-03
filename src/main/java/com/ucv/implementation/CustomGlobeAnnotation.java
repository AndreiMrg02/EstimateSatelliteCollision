package com.ucv.implementation;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.GlobeAnnotation;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static com.ucv.controller.EarthViewController.wwd;

public class CustomGlobeAnnotation {

    public void addContinentAnnotations() {
        AnnotationLayer annotationLayer = new AnnotationLayer();

        Map<String, Position> continents = new HashMap<>();
        continents.put("Europa", Position.fromDegrees(54.5260, 15.2551));
        continents.put("Asia", Position.fromDegrees(34.0479, 100.6197));
        continents.put("Africa", Position.fromDegrees(8.7832, 34.5085));
        continents.put("America de Nord", Position.fromDegrees(54.5260, -105.2551));
        continents.put("America de Sud", Position.fromDegrees(-8.7832, -55.4915));
        continents.put("Antarctica", Position.fromDegrees(-82.8628, 135.0));
        continents.put("Australia", Position.fromDegrees(-25.2744, 133.7751));

        for (Map.Entry<String, Position> entry : continents.entrySet()) {
            GlobeAnnotation annotation = new GlobeAnnotation(entry.getKey(), entry.getValue());

            AnnotationAttributes attributes = styleAttributeAnnotation();

            annotation.setAttributes(attributes);
            annotationLayer.addAnnotation(annotation);
        }

        wwd.getModel().getLayers().add(annotationLayer);
    }

    private AnnotationAttributes styleAttributeAnnotation() {
        AnnotationAttributes attributes = new AnnotationAttributes();
        attributes.setCornerRadius(0);
        attributes.setBackgroundColor(new Color(0, 0, 0, 0));
        attributes.setTextColor(Color.WHITE);
        attributes.setDrawOffset(new Point(0, 0));
        attributes.setBorderWidth(0);
        attributes.setInsets(new Insets(0, 0, 0, 0));
        attributes.setLeader(AVKey.SHAPE_NONE);
        attributes.setFont(Font.decode("Arial-BOLD-14"));
        attributes.setDistanceMinScale(0.8);
        attributes.setDistanceMaxScale(0.8);
        attributes.setDistanceMinOpacity(1.0);
        return attributes;
    }
    public void attributeSatelliteNameLabel(GlobeAnnotation label) {
        label.getAttributes().setCornerRadius(0);
        label.getAttributes().setBackgroundColor(new Color(0, 0, 0, 0)); // Complet transparent
        label.getAttributes().setTextColor(Color.pink);
        label.getAttributes().setDrawOffset(new Point(0, 0));
        label.getAttributes().setBorderWidth(0);
        label.getAttributes().setInsets(new Insets(0, 0, 0, 0));
        label.getAttributes().setLeader(AVKey.SHAPE_NONE);
        label.getAttributes().setFont(Font.decode("Arial-BOLD-14"));
        label.getAttributes().setDistanceMinScale(0.8);  // Menține dimensiunea textului constantă
        label.getAttributes().setDistanceMaxScale(0.8);  // Opțional, limitează scalarea maximă
        label.getAttributes().setDistanceMinOpacity(1.0);  // Menține opacitatea constantă
    }
}
