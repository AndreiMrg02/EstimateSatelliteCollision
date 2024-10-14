package com.ucv.controller;

import com.ucv.util.LoggerCustom;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.*;

public class EarthViewController extends ApplicationTemplate implements Initializable, Runnable {
    public static final WorldWindow wwd = new WorldWindowGLJPanel();
    private final Logger logger = LogManager.getLogger(EarthViewController.class);
    @FXML
    private StackPane earthPanel;
    private Map<String, Ephemeris> ephemerisMap;
    private Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;
    private OneAxisEllipsoid earth;
    private AirspaceLayer satAirspaces;
    private AnnotationLayer labelLayer; // Adăugarea layer-ului pentru etichete
    private volatile boolean pause;
    private volatile boolean restart;
    private volatile boolean stop;
    private volatile boolean isCollision;
    private AbsoluteDate closeApproachDate;
    private Thread simulationThread;
    private AbsoluteDate targetDate;
    private Map<String, List<Airspace>> sphereFragmentsMap;
    private SatelliteInformationUpdate updateSatellitesInformation;

    public EarthViewController() {
        try {
            File orekitData = new File("data/orekit-data");
            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new DirectoryCrawler(orekitData));

            this.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
            this.sphereMap = new HashMap<>();
            this.ephemerisMap = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUpdateSatellitesInformation(SatelliteInformationUpdate updateSatellitesInformation) {
        this.updateSatellitesInformation = updateSatellitesInformation;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {

            logger.info("Initializing WorldWind");
            WorldWind.setOfflineMode(true);
            wwd.setModel(new BasicModel());
            sphereFragmentsMap = new HashMap<>();
            SwingNode swingNode = new SwingNode();
            swingNode.setContent((WorldWindowGLJPanel) wwd);
            swingNode.setVisible(true);
            addContinentAnnotations();
            StackPane borderPane = (StackPane) earthPanel.lookup("#earthPanel");
            borderPane.getChildren().add(swingNode);
            isCollision = false;
        } catch (Exception e) {
            logger.error("An error occurred during earth initialization", e);
        }
    }

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

    public synchronized void init(Map<String, Ephemeris> ephemerisMap, AbsoluteDate startDate, AbsoluteDate endDate, AbsoluteDate closeApproach) {
        this.ephemerisMap = ephemerisMap != null ? ephemerisMap : new HashMap<>();
        this.startDate = startDate;
        this.endDate = endDate;
        this.satAirspaces = new AirspaceLayer();
        this.sphereMap = new HashMap<>();
        this.closeApproachDate = closeApproach;
        this.labelLayer = new AnnotationLayer();

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
            attributeSatelliteNameLabel(label);
            sphereMap.put(entry.getKey(), new AbstractMap.SimpleEntry<>(sphere, label));

            satAirspaces.addAirspace(sphere);
            labelLayer.addAnnotation(label);

        }

        wwd.getModel().getLayers().add(satAirspaces);
        wwd.getModel().getLayers().add(labelLayer);
        wwd.redraw();
    }

    private void attributeSatelliteNameLabel(GlobeAnnotation label) {
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

    public synchronized void setStartDate(AbsoluteDate startDate) {
        this.startDate = startDate;
        this.targetDate = startDate; // Actualizează și targetDate
    }

    @Override
    public void run() {
        if (setAbsoluteDateOnThread()) return;

        while (!stop) {
            synchronized (this) {
                while (pause) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            AbsoluteDate finalTargetDate = targetDate;
            Platform.runLater(() -> {
                updateSatellites(finalTargetDate);
                wwd.redraw();
            });

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            targetDate = targetDate.shiftedBy(30);
            if (restart) {
                targetDate = startDate; 
                restart = false;
            }
        }
    }

    private boolean setAbsoluteDateOnThread() {
        stop = false;
        restart = false;

        if (this.earth == null) {
            return true;
        }

        AbsoluteDate minDate = null;
        AbsoluteDate maxDate = null;

        for (Ephemeris ephemeris : ephemerisMap.values()) {
            if (minDate == null || ephemeris.getMinDate().compareTo(minDate) < 0) {
                minDate = ephemeris.getMinDate();
            }
            if (maxDate == null || ephemeris.getMaxDate().compareTo(maxDate) > 0) {
                maxDate = ephemeris.getMaxDate();
            }
        }

        if (minDate == null || maxDate == null) {
            LoggerCustom.getInstance().logMessage("No available data for propagation");
            return true;
        }

        if (startDate.compareTo(minDate) < 0) {
            startDate = minDate;
        }
        if (endDate.compareTo(maxDate) > 0) {
            endDate = maxDate;
        }
        targetDate = startDate;
        return false;
    }

    public void updateSatellites(AbsoluteDate targetDate) {
        Map<String, Vector3D> positions = new HashMap<>();
        AbsoluteDate threeMinutesAfter = closeApproachDate.shiftedBy(-180);
        AbsoluteDate threeMinutesBefore = closeApproachDate.shiftedBy(180);

        ephemerisMap.forEach((name, ephemeris) -> {
            try {
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
                        shatterSphere(sphere, name);
                    }
                }
            } catch (OrekitException e) {
                logger.error(String.format("Error updating satellites for date: %s and satellite: %s", targetDate, name), e);
            }
        });
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

    private void createSphere(String name, Map.Entry<SphereAirspace, GlobeAnnotation> entry) {
        if (entry == null) {
            SphereAirspace sphere = new SphereAirspace();
            sphere.setRadius(100000);
            GlobeAnnotation label = new GlobeAnnotation(name, new Position(sphere.getLocation(), sphere.getRadius() * 1.2));
            sphereMap.put(name, new AbstractMap.SimpleEntry<>(sphere, label));
            satAirspaces.addAirspace(sphere);
        }
    }

    public void triggerCollision(boolean verdict) {
        isCollision = verdict;
    }

    private void shatterSphere(SphereAirspace sphere, String sphereId) {
        Random random = new Random();
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

    public synchronized void startSimulation() {
        if (simulationThread == null || !simulationThread.isAlive()) {
            simulationThread = new Thread(this);
            simulationThread.start();
        }
    }

    public AbsoluteDate getCloseApproachDate() {
        return closeApproachDate;
    }

    public synchronized void stopSimulation() {
        stop = true;
        pause = false;
        restart = false;
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
        }
    }

    public synchronized void pauseSimulation() {
        pause = true;
    }

    public synchronized void resumeSimulation() {
        pause = false;
        notifyAll();
    }

    public synchronized void resetState() {
        stopSimulation();
        if (satAirspaces != null) {
            satAirspaces.removeAllAirspaces();
        }
        if (labelLayer != null) {
            labelLayer.removeAllAnnotations();
        }
        if (sphereMap != null) {
            sphereMap.clear();
        }
        ephemerisMap = null;
        startDate = null;
        endDate = null;
        closeApproachDate = null;
        Platform.runLater(wwd::redraw);
    }


    public synchronized void delete() {
        resetState();
        LoggerCustom.getInstance().logMessage("Spheres have been removed and simulation stopped!");
    }

    public OneAxisEllipsoid getEarth() {
        return earth;
    }

}
