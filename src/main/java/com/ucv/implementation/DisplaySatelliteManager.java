package com.ucv.implementation;

import com.ucv.controller.EarthViewController;
import com.ucv.controller.SatelliteController;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplaySatelliteManager {
    private final SatelliteController satelliteController;
    private final EarthViewController earthViewController;

    public DisplaySatelliteManager(SatelliteController satelliteController, EarthViewController earthViewController){
        this.satelliteController = satelliteController;
        this.earthViewController = earthViewController;
    }

    public void displaySatellites() {
        List<DisplaySatelliteModel> satellites = satelliteController.getTwoSatellitesSelected();
        if (satellites == null || satellites.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No satellites data are available. Please select an entry in table.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        earthViewController.resetState();
        Map<String, Ephemeris> ephemerisMap = new HashMap<>();
        AbsoluteDate startDate = null;
        AbsoluteDate endDate = null;
        AbsoluteDate closeApproach = null;
        for (DisplaySatelliteModel model : satellites) {
            ephemerisMap.put(model.getName(), model.getEphemeris());
            if (startDate == null || model.getStartDate().compareTo(startDate) < 0) {
                startDate = model.getStartDate();
            }
            if (endDate == null || model.getEndDate().compareTo(endDate) > 0) {
                endDate = model.getEndDate();
            }
            closeApproach = model.getCloseApproachDate();
        }
        earthViewController.init(ephemerisMap, startDate, endDate, closeApproach);
        earthViewController.startSimulation();
    }
    public void drawEarthAfterInit() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                EarthViewController.wwd.redraw();
            }
        };
        timer.start();
    }
}
