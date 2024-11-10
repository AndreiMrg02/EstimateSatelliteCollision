package com.ucv.handler;

import com.ucv.controller.SatelliteController;
import com.ucv.controller.SatelliteInformationController;
import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import com.ucv.tle.TleService;
import com.ucv.util.CustomAlert;
import com.ucv.util.LoggerCustom;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.Map;

public class SatelliteTaskHandler {

    private final CustomAlert customAlert;
    private final TleService tleService;
    private final InternetConnectionData connectionData;
    private final SatelliteController satelliteController;
    private final SatelliteInformationController satelliteInformationController;
    private final ProgressIndicator progressBar;
    private final BorderPane mainPanel;
    private final String threshHoldValue;

    public SatelliteTaskHandler(TleService tleService, InternetConnectionData connectionData, SatelliteController satelliteController,
                                SatelliteInformationController satelliteInformationController, ProgressIndicator progressBar,
                                BorderPane mainPanel, String threshHoldValue) {
        this.tleService = tleService;
        this.connectionData = connectionData;
        this.satelliteController = satelliteController;
        this.satelliteInformationController = satelliteInformationController;
        this.progressBar = progressBar;
        this.mainPanel = mainPanel;
        this.customAlert = new CustomAlert();
        this.threshHoldValue = threshHoldValue;
    }

    public Task<Void> createSatelliteDataTask(Map<String, String[]> tleData, String operator, MouseEvent event,
                                              TextArea valueField, RadioButton spaceTrackTleRadio,
                                              StackPane informationPane, Button showSatellitesButton) {
        return new Task<>() {
            @Override
            protected Void call() {
                LoggerCustom.getInstance().logMessage("INFO: The process for extracting the data is running...");

                Platform.runLater(SatelliteTaskHandler.this::displayProgressBar);
                CollectSatelliteData collectSatelliteData = new CollectSatelliteData(connectionData);
                Map<String, Item> satelliteData = collectSatelliteData.extractSatelliteData("MIN_RNG", operator, valueField.getText());

                if (satelliteData.get("InvalidCredentials") != null) {
                    Platform.runLater(() -> {
                        customAlert.alertInvalidCredentials();
                        event.consume();
                    });
                    return null;
                }
                satelliteController.setListOfUniqueSatellite(satelliteData);
                if (!tleService.downloadTLEs(satelliteData, tleData, collectSatelliteData, spaceTrackTleRadio, informationPane)) {
                    cancelTask(showSatellitesButton);
                    return null;
                }
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    if (satelliteData.isEmpty()) {
                        customAlert.alertNoResults(satelliteController, mainPanel);
                        event.consume();
                    }
                    setTaskOnSuccess(showSatellitesButton, informationPane);
                });

                return null;
            }
        };
    }

    private void cancelTask(Button showSatellitesButton) {
        LoggerCustom.getInstance().logMessage("INFO: Task was cancelled.");
        Platform.runLater(() -> {
            progressBar.setVisible(false);
            mainPanel.setDisable(false);
            showSatellitesButton.setDisable(true);
        });
    }

    private void displayProgressBar() {
        mainPanel.setDisable(true);
        progressBar.toFront();
        progressBar.setVisible(true);
        progressBar.setStyle("-fx-accent: #ff0000;");
        progressBar.setScaleX(1.5);
        progressBar.setOpacity(1.0);
        progressBar.setScaleY(1.5);
    }

    public void setTaskOnSuccess(Button showSatellitesButton, StackPane informationPane) {
        mainPanel.setDisable(false);
        satelliteController.getSatelliteTable().refresh();
        informationPane.setVisible(true);
        updateCollisionInformation(showSatellitesButton);
        LoggerCustom.getInstance().logMessage("INFO: Satellite's data were downloaded.");
    }

    private void updateCollisionInformation(Button showSatellitesButton) {
        satelliteController.getSatelliteTable().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showSatellitesButton.setDisable(false);
                satelliteInformationController.setCollisionInformation(newValue, threshHoldValue);  // Use appropriate value for threshold
            }
        });
    }
}
