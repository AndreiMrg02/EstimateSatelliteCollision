package com.ucv.controller;

import com.ucv.earth.AbsoluteDateHandler;
import com.ucv.earth.ComponentEarthInit;
import com.ucv.helper.SatelliteUpdaterHelper;
import com.ucv.implementation.CustomGlobeAnnotation;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

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
    private AnnotationLayer labelLayer;
    private volatile boolean pause;
    private volatile boolean restart;
    private volatile boolean stop;
    private volatile boolean isCollision;
    private AbsoluteDate closeApproachDate;
    private Thread simulationThread;
    private AbsoluteDate targetDate;
    private Map<String, List<Airspace>> sphereFragmentsMap;
    private SatelliteInformationUpdate updateSatellitesInformation;
    private CustomGlobeAnnotation customGlobeAnnotation;

    public EarthViewController() {
        try {
            File orekitData = new File("data/orekit-data");
            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new DirectoryCrawler(orekitData));

            this.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
            this.sphereMap = new HashMap<>();
            this.ephemerisMap = new HashMap<>();
            this.customGlobeAnnotation = new CustomGlobeAnnotation();
        } catch (Exception e) {
            logger.error("An unexpected error occurred while loading orekit-data and init variables in the Earth View Controller");
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
            customGlobeAnnotation.addContinentAnnotations();
            StackPane borderPane = (StackPane) earthPanel.lookup("#earthPanel");
            borderPane.getChildren().add(swingNode);
            isCollision = false;
        } catch (Exception e) {
            logger.error("An error occurred during earth initialization", e);
        }
    }

    public synchronized void init(Map<String, Ephemeris> ephemerisMap, AbsoluteDate startDate, AbsoluteDate endDate, AbsoluteDate closeApproach) {
        this.ephemerisMap = ephemerisMap != null ? ephemerisMap : new HashMap<>();
        this.startDate = startDate;
        this.endDate = endDate;
        this.satAirspaces = new AirspaceLayer();
        this.sphereMap = new HashMap<>();
        this.closeApproachDate = closeApproach;
        this.labelLayer = new AnnotationLayer();
        ComponentEarthInit earthInitializer = new ComponentEarthInit(customGlobeAnnotation, sphereMap, satAirspaces, labelLayer);
        earthInitializer.init(ephemerisMap);
    }

    public synchronized void setStartDate(AbsoluteDate startDate) {
        this.startDate = startDate;
        this.targetDate = startDate;
    }

    @Override
    public void run() {
        stop = false;
        restart = false;
        AbsoluteDateHandler absoluteDateHandler = new AbsoluteDateHandler(this.earth, ephemerisMap, startDate, endDate);
        targetDate = absoluteDateHandler.setAbsoluteDateOnThread();
        if (targetDate == null) {
            return;
        }
        while (!stop) {
            handlePause();
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

    private void handlePause() {
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
    }

    public void updateSatellites(AbsoluteDate targetDate) {
        SatelliteUpdaterHelper satelliteUpdaterHelper = new SatelliteUpdaterHelper(earth,ephemerisMap,sphereMap,satAirspaces,labelLayer,sphereFragmentsMap);
        satelliteUpdaterHelper.updateSatellites(targetDate,closeApproachDate,updateSatellitesInformation,isCollision);
    }

    public void triggerCollision(boolean verdict) {
        isCollision = verdict;
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

    public synchronized void pauseSimulation() {
        pause = true;
    }

    public synchronized void resumeSimulation() {
        pause = false;
        notifyAll();
    }

    public synchronized void resetState() {
        stop = true;
        pause = false;
        restart = false;
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
        }
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

}
