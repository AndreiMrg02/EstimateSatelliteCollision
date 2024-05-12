package com.ucv.controller;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.ogc.collada.ColladaPhong;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

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
    Map <String, SphereAirspace> sphereMap;
    Map <String, List<AbsoluteDate[]>> intervalMap;
    AbsoluteDate startDate;
    AbsoluteDate endDate;

    private OneAxisEllipsoid earth;

    private AirspaceLayer satAirspaces;
    private boolean pause;
    private boolean restart;
    private boolean stop;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        wwd = new WorldWindowGLJPanel();
        wwd.setModel(new BasicModel());
        SwingNode swingNode = new SwingNode();
        swingNode.maxWidth(100);
        swingNode.setContent((WorldWindowGLJPanel) wwd);
        swingNode.setVisible(true);

        StackPane borderPane = (StackPane) earthPanel.lookup("#earthPanel");
        borderPane.getChildren().add(swingNode);

    }
    public EarthController() {
        super();

    }

    public synchronized void pause() {
        this.pause = true;
    }

    public synchronized void restart() {
        this.restart = true;
    }

    public synchronized void stop() {
        this.stop = true;
    }

    public synchronized void resume() {
        this.pause = false;
        this.restart = false;
    }

    public synchronized void delete() {
        for (Map.Entry<String, SphereAirspace> spheres : sphereMap.entrySet()) {
            satAirspaces.removeAirspace(spheres.getValue());
        }
    }

    //initialize the thread,like the constructor
    public synchronized void init(Map<String, Ephemeris> ephemerisMap, WorldWindow wwd, OneAxisEllipsoid earth, AbsoluteDate startDate, AbsoluteDate endDate, Map<String, List<AbsoluteDate[]>> intervalMap) {

        this.ephemerisMap = new HashMap<>();
        this.ephemerisMap = ephemerisMap;
        this.intervalMap = intervalMap;
        this.wwd = wwd;
        this.earth = earth;
        this.pause = false;
        this.restart = false;
        this.stop = false;
        this.satAirspaces = new AirspaceLayer();
        this.startDate = startDate;
        this.endDate = endDate;
        this.sphereMap = new HashMap<>();

        for (Map.Entry<String, Ephemeris> pair : ephemerisMap.entrySet()) {
            sphereMap.put(pair.getKey(), new SphereAirspace());
        }

        wwd.getModel().getLayers().add(satAirspaces);
        //add each sphere from the map into our airspace layer
        for (Map.Entry<String, SphereAirspace> spheres : sphereMap.entrySet()) {
            satAirspaces.addAirspace(spheres.getValue());
        }
        wwd.redraw();
    }

    @Override
    public void run() {
        while (!stop) {
            AbsoluteDate targetDate = startDate;
            //while loop where we go from startDate to endDate parsing it minute to minute
            while (targetDate.compareTo(endDate) <= 0) {
                //parse our ephemeris map

                for (Map.Entry<String, Ephemeris> pair : ephemerisMap.entrySet()) {
                    Ephemeris satteliteEphemeris = pair.getValue();
                    //propagate ephemeris & get spacecraftstate from propagation
                    SpacecraftState propagateState = satteliteEphemeris.propagate(targetDate);
                    //get the orbit
                    //exception in thread-> orbit not defined
                    Orbit orbit = propagateState.getOrbit();

                    try {
                        //compute GeodeticPoint for each satellite orbit
                        final GeodeticPoint gp = earth.transform(orbit.getPVCoordinates().getPosition(), orbit.getFrame(), orbit.getDate());
                        SphereAirspace sa = sphereMap.get(pair.getKey());
                        //set a static radius and color
                        sa.setRadius(580000.0);
                        List<AbsoluteDate[]> intervals = intervalMap.get(pair.getKey());
                        sa.getAttributes().setMaterial(Material.MAGENTA);
                        for (AbsoluteDate[] inter : intervals) {
                            if (inter[0].compareTo(targetDate) <= 0 && inter[1].compareTo(targetDate) >= 0) {
                                sa.getAttributes().setMaterial(Material.GREEN);
                                break;
                            }

                        }
                        sa.setLocation(LatLon.fromRadians(gp.getLatitude(), gp.getLongitude()));
                        sa.setAltitude(gp.getAltitude());

                    } catch (OrekitException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                wwd.redraw();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (!this.pause) {
                    targetDate = targetDate.shiftedBy(60);
                }

                if (this.restart) {
                    targetDate = startDate;
                }

            }


        }


    }
}
