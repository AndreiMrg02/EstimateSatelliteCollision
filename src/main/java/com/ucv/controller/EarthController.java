package com.ucv.controller;

import com.ucv.Util.ToolTipController;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.analytical.KeplerianPropagator;
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
    Map<String, Ephemeris> ephemerisMap;
    Map<String, SphereAirspace> sphereMap;
    Map<String, List<AbsoluteDate[]>> intervalMap;
    AbsoluteDate startDate;
    AbsoluteDate endDate;

    private OneAxisEllipsoid earth;

    private AirspaceLayer satAirspaces;
    private boolean pause;
    private boolean restart;
    private boolean stop;
    private String satName;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        wwd = new WorldWindowGLJPanel();
        wwd.setModel(new BasicModel());
        SwingNode swingNode = new SwingNode();
        swingNode.setContent((WorldWindowGLJPanel) wwd);
        swingNode.setVisible(true);

        StackPane borderPane = (StackPane) earthPanel.lookup("#earthPanel");
        borderPane.getChildren().add(swingNode);

        setupToolTip(wwd);

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setupToolTip(WorldWindow wwd) {
        wwd.addSelectListener(event -> {
            if (event.getEventAction().equals(SelectEvent.HOVER)) {
                if (event.getTopObject() instanceof SphereAirspace) {
                    SphereAirspace sphere = (SphereAirspace) event.getTopObject();
                    String satelliteName = (String) sphere.getValue(AVKey.DISPLAY_NAME);
                    javafx.geometry.Point2D screenLocation = new javafx.geometry.Point2D(event.getMouseEvent().getX(), event.getMouseEvent().getY());
                    ToolTipController.showToolTip(screenLocation, satelliteName);
                } else {
                    ToolTipController.hideToolTip();
                }
            }
        });
    }

    public synchronized void init(Map<String, Ephemeris> ephemerisMap, WorldWindow wwd, OneAxisEllipsoid earth, AbsoluteDate startDate, AbsoluteDate endDate, Map<String, List<AbsoluteDate[]>> intervalMap) {
        this.ephemerisMap = ephemerisMap;
        this.intervalMap = intervalMap;
        this.wwd = wwd;
        this.earth = earth;
        this.startDate = startDate;
        this.endDate = endDate;
        this.satAirspaces = new AirspaceLayer();
        this.sphereMap = new HashMap<>();

        System.out.println("Creating spheres for each satellite.");
        for (Map.Entry<String, Ephemeris> entry : ephemerisMap.entrySet()) {
            SphereAirspace sphere = new SphereAirspace();
            sphere.setRadius(280000);
            sphere.setAttributes(new BasicAirspaceAttributes(new Material(Color.GREEN), 1.0));
            sphere.setValue(AVKey.DISPLAY_NAME, entry.getKey());
            sphereMap.put(entry.getKey(), sphere);
            satAirspaces.addAirspace(sphere);
        }

        wwd.getModel().getLayers().add(satAirspaces);
        wwd.redraw();

        System.out.println("Spheres added to airspace layer.");
    }


    @Override
    public void run() {
        while (!stop) {
            AbsoluteDate targetDate = startDate;

            // Verifică dacă obiectul earth este inițializat
            if (this.earth == null) {
                System.out.println("The Earth object is not initialized.");
                return;
            }

            // Verifică intervalul de date disponibile pentru propagare
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

            // Ajustează startDate și endDate dacă sunt în afara intervalului disponibil
            if (startDate.compareTo(minDate) < 0) {
                startDate = minDate;
            }
            if (endDate.compareTo(maxDate) > 0) {
                endDate = maxDate;
            }

            while (targetDate.compareTo(endDate) <= 0) {
                // Update satellite positions and attributes at the current target date
                updateSatellites(targetDate);

                wwd.redraw();

                try {
                    Thread.sleep(100); // Controlează viteza de simulare (100 ms între fiecare pas de timp)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!this.pause) {
                    targetDate = targetDate.shiftedBy(60); // Propaga la fiecare minut
                }

                if (this.restart) {
                    targetDate = startDate; // Resetare la data de început dacă este necesar
                }
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
                        AirspaceAttributes attrs = new BasicAirspaceAttributes();
                        attrs.setDrawOutline(true);
                        attrs.setMaterial(new Material(Color.GREEN)); // Puteți specifica culoarea materialului aici
                        sphere.setAttributes(attrs);
                        sphere.setRadius(280000); // Modifică în funcție de nevoile tale
                        sphere.setValue(AVKey.DISPLAY_NAME, name);
                        sphereMap.put(name, sphere);
                        satAirspaces.addAirspace(sphere);
                    }
                    sphere.setLocation(LatLon.fromRadians(gp.getLatitude(), gp.getLongitude()));
                    sphere.setAltitude(gp.getAltitude());
                }
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        });
    }

    public OneAxisEllipsoid getEarth() {
        return earth;
    }

    public void setEarth(OneAxisEllipsoid earth) {
        this.earth = earth;
    }
}
