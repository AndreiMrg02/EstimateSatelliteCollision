package com.ucv.controller;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.ogc.collada.impl.ColladaController;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
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

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class EarthController extends ApplicationTemplate implements Initializable, Runnable {
    protected static WorldWindow wwd;
    @FXML
    private StackPane earthPanel;
    private Map<String, Ephemeris> ephemerisMap;
    private Map<String, SphereAirspace> sphereMap;
    private Map<String, List<AbsoluteDate[]>> intervalMap;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;
    private OneAxisEllipsoid earth;
    private AirspaceLayer satAirspaces;
    private volatile boolean pause;
    private volatile boolean restart;
    private volatile boolean stop;

    private AbsoluteDate closeApproachDate;
    private Thread simulationThread;
    private AbsoluteDate targetDate;
    private Map<String, Path> pathMap;



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        wwd = new WorldWindowGLJPanel();
        wwd.setModel(new BasicModel());
        SwingNode swingNode = new SwingNode();
        swingNode.setContent((WorldWindowGLJPanel) wwd);
        swingNode.setVisible(true);

        StackPane borderPane = (StackPane) earthPanel.lookup("#earthPanel");
        borderPane.getChildren().add(swingNode);

        System.out.println("Initialization complete.");
    }

    public EarthController() {
        // Inițializează obiectul earth
        try {
            File orekitData = new File("data/orekit-data");
            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new DirectoryCrawler(orekitData));

            // Inițializează obiectul earth cu datele necesare
            this.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            // Inițializează hărțile
            this.sphereMap = new HashMap<>();
            this.ephemerisMap = new HashMap<>();
            this.intervalMap = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void init(Map<String, Ephemeris> ephemerisMap, WorldWindow wwd, OneAxisEllipsoid earth, AbsoluteDate startDate, AbsoluteDate endDate, Map<String, List<AbsoluteDate[]>> intervalMap, AbsoluteDate closeApproach) {
        this.ephemerisMap = ephemerisMap != null ? ephemerisMap : new HashMap<>();
        this.intervalMap = intervalMap != null ? intervalMap : new HashMap<>();
        this.earth = earth;
        this.startDate = startDate;
        this.endDate = endDate;
        this.satAirspaces = new AirspaceLayer();
        this.sphereMap = new HashMap<>();

        System.out.println("Creating spheres for each satellite.");
        for (Map.Entry<String, Ephemeris> entry : ephemerisMap.entrySet()) {
            SphereAirspace sphere = new SphereAirspace();
            sphere.setRadius(150000);
            sphere.setAttributes(new BasicAirspaceAttributes(new Material(Color.GREEN), 1.0));
            sphere.setValue(AVKey.DISPLAY_NAME, entry.getKey());
            satAirspaces.setName(entry.getKey());
            sphereMap.put(entry.getKey(), sphere);
            satAirspaces.addAirspace(sphere);
            satAirspaces.setEnabled(true);


        }
        closeApproachDate = closeApproach;
        targetDate = startDate; // Inițializează targetDate
        wwd.getModel().getLayers().add(satAirspaces);
        wwd.redraw();

        System.out.println("Spheres added to airspace layer.");
    }


    public synchronized void setStartDate(AbsoluteDate startDate) {
        this.startDate = startDate;
        this.targetDate = startDate; // Actualizează și targetDate
    }



    @Override
    public void run() {
        stop = false;
        restart = false;

        if (this.earth == null) {
            System.out.println("The Earth object is not initialized.");
            return;
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
            System.out.println("No available data for propagation.");
            return;
        }

        if (startDate.compareTo(minDate) < 0) {
            startDate = minDate;
        }
        if (endDate.compareTo(maxDate) > 0) {
            endDate = maxDate;
        }

        targetDate = startDate;

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

            // Actualizarea sateliților și re-redarea pe thread-ul corect
            AbsoluteDate finalTargetDate = targetDate;
            Platform.runLater(() -> {
                updateSatellites(finalTargetDate);
                wwd.redraw();
                System.out.println("Redrawing at target date: " + finalTargetDate);
            });

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            targetDate = targetDate.shiftedBy(60); // Propaga la fiecare minut

            if (restart) {
                targetDate = startDate; // Resetare la data de început dacă este necesar
                restart = false;
            }
        }
    }




    public void updateSatellites(AbsoluteDate targetDate) {
        ephemerisMap.forEach((name, ephemeris) -> {
            try {
                if (targetDate.compareTo(ephemeris.getMinDate()) >= 0 && targetDate.compareTo(ephemeris.getMaxDate()) <= 0) {
                    SpacecraftState state = ephemeris.propagate(targetDate);
                    Orbit orbit = state.getOrbit();
                    GeodeticPoint gp = earth.transform(orbit.getPVCoordinates().getPosition(), orbit.getFrame(), orbit.getDate());
                    SphereAirspace sphere = sphereMap.get(name);

                    if (sphere == null) {
                        sphere = new SphereAirspace();
                        sphere.setRadius(150000); // Modifică în funcție de nevoile tale
                        sphere.setValue(AVKey.DISPLAY_NAME, name);
                        sphereMap.put(name, sphere);
                        satAirspaces.addAirspace(sphere);
                        satAirspaces.setName(name);
                        System.out.println("New sphere created for satellite: " + name);
                    }

                    // Actualizare atribute
                    AirspaceAttributes attrs = new BasicAirspaceAttributes();
                    attrs.setDrawOutline(true);
                    attrs.setMaterial(new Material(Color.GREEN));

                    // Verifică perioada de apropiere și schimbă culoarea
                    AbsoluteDate oneHourEarlier = closeApproachDate.shiftedBy(-3600); // 3600 secunde = 1 oră
                    AbsoluteDate oneHourLater = closeApproachDate.shiftedBy(3600);

                    if (targetDate.compareTo(oneHourEarlier) >= 0 && targetDate.compareTo(oneHourLater) <= 0) {
                        attrs.setMaterial(new Material(Color.RED));
                    }

                    sphere.setAttributes(attrs);
                    sphere.setLocation(LatLon.fromRadians(gp.getLatitude(), gp.getLongitude()));
                    sphere.setAltitude(gp.getAltitude());
                    sphere.setValue(AVKey.DISPLAY_NAME, name);
                    satAirspaces.setName(name);
                    satAirspaces.setEnabled(true);

                    System.out.println("Updated sphere for satellite: " + name + " at lat: " + gp.getLatitude() + ", lon: " + gp.getLongitude() + ", alt: " + gp.getAltitude());
                }
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        });
    }


    public synchronized AbsoluteDate getTargetDate() {
        return targetDate;
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
        notifyAll(); // Trezește thread-ul de simulare dacă este în pauză
    }
    public synchronized void resetState() {
        stopSimulation();
        if (satAirspaces != null) {
            satAirspaces.removeAllAirspaces();
        }
        if (sphereMap != null) {
            sphereMap.clear();
        }
        ephemerisMap = null;
        intervalMap = null;
        startDate = null;
        endDate = null;
        closeApproachDate = null;
        Platform.runLater(() -> wwd.redraw());
    }

    public synchronized void resetSimulation() {
        restart = true;
        pause = false;
    }

    public synchronized void delete() {
        resetState();
        System.out.println("Spheres have been removed and simulation stopped!");
    }


    public OneAxisEllipsoid getEarth() {
        return earth;
    }

    public void setEarth(OneAxisEllipsoid earth) {
        this.earth = earth;
    }
}
