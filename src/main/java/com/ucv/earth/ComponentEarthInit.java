package com.ucv.earth;

import com.ucv.implementation.CustomGlobeAnnotation;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import org.orekit.propagation.analytical.Ephemeris;

import java.awt.*;
import java.util.AbstractMap;
import java.util.Map;

import static com.ucv.controller.EarthViewController.wwd;

public class ComponentEarthInit {

    private final CustomGlobeAnnotation customGlobeAnnotation;
    private final Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap;
    private final AirspaceLayer satAirspace;
    private final AnnotationLayer labelLayer;

    public ComponentEarthInit(CustomGlobeAnnotation customGlobeAnnotation, Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap, AirspaceLayer satAirspace, AnnotationLayer labelLayer) {

        this.customGlobeAnnotation = customGlobeAnnotation;
        this.sphereMap = sphereMap;
        this.satAirspace = satAirspace;
        this.labelLayer = labelLayer;
    }


    public synchronized void init(Map<String, Ephemeris> ephemerisMap) {

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

            satAirspace.addAirspace(sphere);
            labelLayer.addAnnotation(label);

        }

        wwd.getModel().getLayers().add(satAirspace);
        wwd.getModel().getLayers().add(labelLayer);
        wwd.redraw();
    }

}
