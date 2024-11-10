package com.ucv.implementation;

import com.ucv.controller.EarthViewController;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import javafx.animation.AnimationTimer;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.awt.*;
import java.util.AbstractMap;
import java.util.Map;

import static com.ucv.controller.EarthViewController.wwd;

public class EarthInitializer {

    private final CustomGlobeAnnotation customGlobeAnnotation;
    private final Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap;
    private final AirspaceLayer satAirspaces;
    private final AnnotationLayer labelLayer;

    public EarthInitializer(CustomGlobeAnnotation customGlobeAnnotation, Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap, AirspaceLayer satAirspaces, AnnotationLayer labelLayer) {
        this.customGlobeAnnotation = customGlobeAnnotation;
        this.sphereMap = sphereMap;
        this.satAirspaces = satAirspaces;
        this.labelLayer = labelLayer;
    }


    public synchronized void init(Map<String, Ephemeris> ephemerisMap, AbsoluteDate startDate, AbsoluteDate endDate, AbsoluteDate closeApproach) {

        if (ephemerisMap == null) {
            return;
        }
        for (Map.Entry<String, Ephemeris> entry : ephemerisMap.entrySet()) {
            SphereAirspace sphere = new SphereAirspace();
            sphere.setRadius(100000);
            sphere.setAttributes(new BasicAirspaceAttributes(new Material(Color.GREEN), 1.0));

            double labelHeight = sphere.getRadius() * 1.2;
            Position labelPos = new Position(sphere.getLocation(), labelHeight);

            GlobeAnnotation label = new GlobeAnnotation(entry.getKey(), labelPos);
            customGlobeAnnotation.attributeSatelliteNameLabel(label);
            sphereMap.put(entry.getKey(), new AbstractMap.SimpleEntry<>(sphere, label));

            satAirspaces.addAirspace(sphere);
            labelLayer.addAnnotation(label);

        }

        wwd.getModel().getLayers().add(satAirspaces);
        wwd.getModel().getLayers().add(labelLayer);
        wwd.redraw();
    }

}
