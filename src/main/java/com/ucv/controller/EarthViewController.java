package com.ucv.controller;

import com.ucv.implementation.CustomGlobeAnnotation;
import com.ucv.implementation.EarthInitializer;
import com.ucv.implementation.SatelliteUpdaterOnEarth;
import com.ucv.util.LoggerCustom;
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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
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
    private EarthInitializer earthInitializer;

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
        earthInitializer = new EarthInitializer(customGlobeAnnotation, sphereMap, satAirspaces, labelLayer);
        earthInitializer.init(ephemerisMap, startDate, endDate, closeApproach);
    }

    public synchronized void setStartDate(AbsoluteDate startDate) {
        this.startDate = startDate;
        this.targetDate = startDate;
    }

    @Override
    public void run() {
        if (setAbsoluteDateOnThread()) return;

        while (!stop) {
            handlePause();

            updateUI(targetDate);

            if (sleepAndCheckInterrupted()) break;

            advanceDate();
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

    public void updateUI(AbsoluteDate date) {
        AbsoluteDate finalTargetDate = date;
        Platform.runLater(() -> {
            updateSatellites(finalTargetDate);
            wwd.redraw();
        });
    }

    private boolean sleepAndCheckInterrupted() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
        return false;
    }

    private void advanceDate() {
        targetDate = targetDate.shiftedBy(30);
        if (restart) {
            targetDate = startDate;
            restart = false;
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
        if (closeApproachDate != null) {
            AbsoluteDate threeMinutesAfter = closeApproachDate.shiftedBy(-180);
            AbsoluteDate threeMinutesBefore = closeApproachDate.shiftedBy(180);

            ephemerisMap.forEach((name, ephemeris) -> {
                try {
                    SatelliteUpdaterOnEarth satelliteUpdaterOnEarth = new SatelliteUpdaterOnEarth(earth, sphereMap, satAirspaces, updateSatellitesInformation, labelLayer, sphereFragmentsMap);
                    satelliteUpdaterOnEarth.processUpdateSatellite(targetDate, name, ephemeris, threeMinutesAfter, threeMinutesBefore, positions, isCollision);
                } catch (OrekitException e) {
                    logger.error(String.format("Error updating satellites for date: %s and satellite: %s", targetDate, name), e);
                }
            });
        }
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
