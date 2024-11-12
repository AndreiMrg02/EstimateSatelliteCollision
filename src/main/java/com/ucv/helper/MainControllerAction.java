package com.ucv.helper;

import com.ucv.controller.EarthViewController;
import com.ucv.controller.SatelliteController;
import com.ucv.controller.SatelliteInformationController;
import com.ucv.implementation.DisplaySatelliteManager;
import com.ucv.util.LoggerCustom;
import javafx.scene.control.Button;
import org.orekit.time.AbsoluteDate;

public class MainControllerAction {

    private final Button stopSimulationButton;
    private final Button simulateCollision;
    private final Button resumeButton;
    private final Button closeApproachButton;
    private final Button pauseButton;
    private final Button showSatellitesButton;
    private final Button extractDataButton;

    public MainControllerAction(Button stopSimulationButton, Button simulateCollision, Button resumeButton, Button closeApproachButton,
                                Button pauseButton, Button showSatellitesButton, Button extractDataButton) {
        this.stopSimulationButton = stopSimulationButton;
        this.simulateCollision = simulateCollision;
        this.resumeButton = resumeButton;
        this.closeApproachButton = closeApproachButton;
        this.pauseButton = pauseButton;
        this.showSatellitesButton = showSatellitesButton;
        this.extractDataButton = extractDataButton;
    }

    public void pauseAction(EarthViewController earthViewController) {

        pauseButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: Simulation paused");
            earthViewController.pauseSimulation();
            resumeButton.setDisable(false);
            pauseButton.setDisable(true);
        });
    }

    public void closeApproachAction(EarthViewController earthViewController) {
        closeApproachButton.setOnAction(event -> showSatellitesAtCloseApproach(earthViewController));
        resumeButton.setOnAction(event -> {
            earthViewController.resumeSimulation();
            resumeButton.setDisable(true);
            pauseButton.setDisable(false);
            showSatellitesButton.setDisable(true);
        });
    }

    public void simulateCollisionAction(EarthViewController earthViewController, SatelliteInformationController satelliteInformationController) {
        simulateCollision.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: A collision was simulated");

            showSatellitesAtCloseApproach(earthViewController);
            pauseButton.setDisable(false);
            resumeButton.setDisable(true);
            satelliteInformationController.clearSatellitesDataFromFields();
            earthViewController.triggerCollision(true);
            earthViewController.pauseSimulation();
            earthViewController.resumeSimulation();
        });
    }

    public void showSatellitesAction(DisplaySatelliteManager displaySatelliteManager, SatelliteController satelliteController) {
        showSatellitesButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: Check the map to see the satellites");
            displaySatelliteManager.displaySatellites();
            simulateCollision.setDisable(false);
            pauseButton.setDisable(false);
            stopSimulationButton.setDisable(false);
            closeApproachButton.setDisable(false);
            showSatellitesButton.setDisable(true);
            satelliteController.getSatelliteTable().setDisable(true);
        });
    }

    public void stopSimulationAction(Button closeButton, EarthViewController earthViewController, SatelliteInformationController satelliteInformationController,
                                     SatelliteController satelliteController) {
        stopSimulationButton.setOnAction(event -> {
            closeButton.setDisable(false);
            LoggerCustom.getInstance().logMessage("INFO: The simulation was stopped");
            earthViewController.resetState();
            showSatellitesButton.setDisable(false);
            simulateCollision.setDisable(true);
            pauseButton.setDisable(true);
            closeApproachButton.setDisable(true);
            stopSimulationButton.setDisable(true);
            satelliteInformationController.clearSatellitesStates();
            resumeButton.setDisable(true);
            extractDataButton.setDisable(false);
            earthViewController.triggerCollision(false);
            satelliteController.getSatelliteTable().setDisable(false);
        });
    }

    public void showSatellitesAtCloseApproach(EarthViewController earthViewController) {
        LoggerCustom.getInstance().logMessage("INFO: The satellites are on the close approach point");
        earthViewController.pauseSimulation();
        AbsoluteDate closeApproach = earthViewController.getCloseApproachDate();
        earthViewController.setStartDate(closeApproach);

        earthViewController.updateSatellites(closeApproach);
        EarthViewController.wwd.redraw();
        extractDataButton.setDisable(true);
        pauseButton.setDisable(true);
        resumeButton.setDisable(false);
    }

}
