package com.ucv.implementation;

import com.ucv.controller.SatelliteInformationUpdate;
import com.ucv.util.LoggerCustom;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.awt.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SatelliteUpdaterOnEarth {
    private final OneAxisEllipsoid earth;
    private final Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap;
    private final AirspaceLayer satAirspaces;
    private final SatelliteInformationUpdate updateSatellitesInformation;
    private final AnnotationLayer labelLayer;
    private final Map<String, List<Airspace>> sphereFragmentsMap;

    public SatelliteUpdaterOnEarth(OneAxisEllipsoid earth,Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap,AirspaceLayer satAirspaces,
                                   SatelliteInformationUpdate updateSatellitesInformation,AnnotationLayer labelLayer,Map<String, List<Airspace>> sphereFragmentsMap){
        this.earth = earth;
        this.sphereMap = sphereMap;
        this.satAirspaces = satAirspaces;
        this.updateSatellitesInformation = updateSatellitesInformation;
        this.labelLayer = labelLayer;
        this.sphereFragmentsMap = sphereFragmentsMap;
    }

    public void processUpdateSatellite(AbsoluteDate targetDate, String name, Ephemeris ephemeris, AbsoluteDate threeMinutesAfter,
                                       AbsoluteDate threeMinutesBefore, Map<String, Vector3D> positions, boolean isCollision) {
        if (targetDate.compareTo(ephemeris.getMinDate()) >= 0 && targetDate.compareTo(ephemeris.getMaxDate()) <= 0) {
            SpacecraftState state = ephemeris.propagate(targetDate);
            Orbit orbit = state.getOrbit();
            PVCoordinates pvCoordinates = orbit.getPVCoordinates();

            GeodeticPoint gp = earth.transform(pvCoordinates.getPosition(), orbit.getFrame(), orbit.getDate());
            Map.Entry<SphereAirspace, GlobeAnnotation> entry = sphereMap.get(name);
            createSphere(name, entry);

            SphereAirspace sphere = entry.getKey();
            GlobeAnnotation label = entry.getValue();

            AirspaceAttributes attrs = new BasicAirspaceAttributes();
            attrs.setDrawOutline(true);
            attrs.setMaterial(new Material(Color.GREEN));

            if (targetDate.compareTo(threeMinutesAfter) >= 0 && targetDate.compareTo(threeMinutesBefore) <= 0) {
                changeSphereOnCloseApproach(name, attrs, sphere, positions, orbit);
            } else {
                sphere.setRadius(100000);
            }
            double speed = pvCoordinates.getVelocity().getNorm(); // m/s
            sphere.setAttributes(attrs);
            sphere.setLocation(LatLon.fromRadians(gp.getLatitude(), gp.getLongitude()));
            sphere.setAltitude(gp.getAltitude());
            // Update label position
            updatePositionAndSatelliteInformation(name, gp, sphere, label, speed,isCollision);
        }
    }

    private void updatePositionAndSatelliteInformation(String name, GeodeticPoint gp, SphereAirspace sphere, GlobeAnnotation label, double speed,boolean isCollision ) {
        Position labelPos = new Position(LatLon.fromRadians(gp.getLatitude(), gp.getLongitude()), gp.getAltitude() + sphere.getRadius() * 1.2);
        label.setPosition(labelPos);
        if (updateSatellitesInformation != null) {
            if (isCollision) {
                updateSatellitesInformation.updateSatelliteInformation("", 0, 0, 0, 0);
            } else {
                updateSatellitesInformation.updateSatelliteInformation(name, FastMath.toDegrees(gp.getLatitude()), FastMath.toDegrees(gp.getLongitude()), FastMath.toDegrees(gp.getAltitude()), speed);
            }
        }
        if (isCollision) {
            labelLayer.removeAllAnnotations();
            SimulateCollision simulateCollision = new SimulateCollision(satAirspaces, sphereFragmentsMap);
            simulateCollision.shatterSphere(sphere, name);
        }
    }

    private void createSphere(String name, Map.Entry<SphereAirspace, GlobeAnnotation> entry) {
        if (entry == null) {
            SphereAirspace sphere = new SphereAirspace();
            sphere.setRadius(100000);
            GlobeAnnotation label = new GlobeAnnotation(name, new Position(sphere.getLocation(), sphere.getRadius() * 1.2));
            sphereMap.put(name, new AbstractMap.SimpleEntry<>(sphere, label));
            satAirspaces.addAirspace(sphere);
        }
    }
    private void changeSphereOnCloseApproach(String name, AirspaceAttributes attrs, SphereAirspace sphere, Map<String, Vector3D> positions, Orbit orbit) {
        attrs.setMaterial(new Material(Color.RED));
        sphere.setRadius(10000);
        positions.put(name, orbit.getPVCoordinates().getPosition());
        if (positions.size() == 2) {
            List<Vector3D> posList = new ArrayList<>(positions.values());
            double distance = posList.get(0).distance(posList.get(1));
            LoggerCustom.getInstance().logMessage(String.format("Distance between satellites: %f meters", distance));
        }
    }

}
