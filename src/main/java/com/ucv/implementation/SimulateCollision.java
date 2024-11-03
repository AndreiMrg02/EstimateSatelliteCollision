package com.ucv.implementation;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import javafx.application.Platform;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.ucv.controller.EarthViewController.wwd;

public class SimulateCollision {
    private AirspaceLayer satAirspaces;
    private Map<String, List<Airspace>> sphereFragmentsMap;
    private static final Random random = new Random(); // Shared Random instance

    public SimulateCollision(AirspaceLayer satAirspaces,Map<String, List<Airspace>> sphereFragmentsMap){
        this.satAirspaces = satAirspaces;
        this.sphereFragmentsMap = sphereFragmentsMap;
    }
    public void shatterSphere(SphereAirspace sphere, String sphereId) {

        int fragments = random.nextInt(15) + 3;
        double fragmentRadius = 15000;

        LatLon sphereLocation = sphere.getLocation();
        double[] altitudes = sphere.getAltitudes();
        clearPreviousFragments(sphereId);

        List<Airspace> newFragments = new ArrayList<>();
        for (int i = 0; i < fragments; i++) {
            SphereAirspace fragment = new SphereAirspace();
            fragment.setRadius(fragmentRadius);
            BasicAirspaceAttributes attrs = new BasicAirspaceAttributes();
            attrs.setMaterial(new Material(Color.GRAY));
            fragment.setAttributes(attrs);

            double angle = random.nextDouble() * 360;
            double distance = random.nextDouble() * sphere.getRadius() * 2 + sphere.getRadius();
            double latOffset = Math.sin(Math.toRadians(angle)) * distance / 6371000;
            double lonOffset = Math.cos(Math.toRadians(angle)) * distance / (6371000 * Math.cos(Math.toRadians(sphereLocation.getLatitude().degrees)));

            LatLon newPos = LatLon.fromRadians(sphereLocation.getLatitude().radians + latOffset, sphereLocation.getLongitude().radians + lonOffset);

            fragment.setLocation(newPos);
            fragment.setAltitudes(altitudes[0], altitudes[1]);

            newFragments.add(fragment);
            satAirspaces.addAirspace(fragment);
        }
        sphereFragmentsMap.put(sphereId, newFragments);
        satAirspaces.removeAirspace(sphere);
        Platform.runLater(wwd::redraw);
    }
    private void clearPreviousFragments(String sphereId) {
        List<Airspace> fragments = sphereFragmentsMap.get(sphereId);
        if (fragments != null) {
            for (Airspace frag : fragments) {
                satAirspaces.removeAirspace(frag);
            }
        }
    }
}
