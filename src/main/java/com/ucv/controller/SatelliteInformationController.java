package com.ucv.controller;

import com.ucv.datamodel.satellite.CollisionData;
import com.ucv.util.PaneCustomStyle;
import com.ucv.util.UtilConstant;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

import static com.ucv.util.UtilConstant.DEGREES;
import static com.ucv.util.UtilConstant.UNKNOWN;

public class SatelliteInformationController implements Initializable, SatelliteInformationUpdate {
    @FXML
    private ProgressIndicator progressBar;
    @FXML
    private StackPane paneInformationCollision;
    @FXML
    private TextArea startDateTextArea;
    @FXML
    private TextArea closeApproachDateTextArea;
    @FXML
    private TextArea endDateTextArea;
    @FXML
    private TextArea closeApproachDistanceTextArea;
    @FXML
    private TextArea thresholdTextArea;
    @FXML
    private TextArea pcTextArea;
    @FXML
    private StackPane satelliteOnePane;
    @FXML
    private Label satelliteOneLabel;
    @FXML
    private TextArea satelliteOneAltitude;
    @FXML
    private TextArea longitudeSatelliteOne;
    @FXML
    private TextArea latitudeSatelliteOne;
    @FXML
    private TextArea speedSatelliteOne;
    @FXML
    private StackPane satelliteTwoPane;
    @FXML
    private Label satelliteTwoLabel;
    @FXML
    private TextArea satelliteTwoAltitude;
    @FXML
    private TextArea longitudeSatelliteTwo;
    @FXML
    private TextArea latitudeSatelliteTwo;
    @FXML
    private TextArea speedSatelliteTwo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roundedPane();
        progressBar.setProgress(-1.0);
        progressBar.setVisible(false);
    }

    public void roundedPane() {
        PaneCustomStyle paneCustomStyle = new PaneCustomStyle();
        paneCustomStyle.addClip(paneInformationCollision, 540, 256, 30, 30);
        paneCustomStyle.addClip(satelliteOnePane, 30, 30);
        paneCustomStyle.addClip(satelliteTwoPane, 30, 30);
    }

    public void setCollisionInformation(CollisionData newValue, String thresholdValue) {

        if (newValue != null) {
            pcTextArea.setText(newValue.getCollisionProbability());
            startDateTextArea.setText(newValue.getStartDate());
            endDateTextArea.setText(newValue.getEndDate());
            closeApproachDateTextArea.setText(newValue.getCloseApproachDate());
            double value = Double.parseDouble(newValue.getCloseApproach()) / 1000;
            closeApproachDistanceTextArea.setText(String.format(UtilConstant.KM_INFO, value ));
            satelliteOneLabel.setText(newValue.getSat1Name());
            satelliteTwoLabel.setText(newValue.getSat2Name());
            if (Integer.parseInt(thresholdValue) == 1) {
                thresholdTextArea.setText(String.format("%s meter", thresholdValue));
            } else {
                thresholdTextArea.setText(String.format("%s meters", thresholdValue));
            }
        }

    }

    @Override
    public void updateSatelliteInformation(String satelliteName, double latitude, double longitude, double altitude, double speed) {
        Platform.runLater(() -> {
            if (satelliteName.isEmpty() && latitude == 0 && longitude == 0 && altitude == 0 && speed == 0) {
                setCollisionText(satelliteName);
            }
            if (satelliteName.equals(satelliteOneLabel.getText())) {
                latitudeSatelliteOne.setText(String.format(DEGREES, latitude));
                longitudeSatelliteOne.setText(String.format(DEGREES, longitude));
                satelliteOneAltitude.setText(String.format(UtilConstant.KM_INFO, altitude / 1000.));
                speedSatelliteOne.setText(String.format("%.2f Km/s", speed / 1000.));
            } else if (satelliteName.equals(satelliteTwoLabel.getText())) {
                latitudeSatelliteTwo.setText(String.format(DEGREES, latitude));
                longitudeSatelliteTwo.setText(String.format(DEGREES, longitude));
                satelliteTwoAltitude.setText(String.format(UtilConstant.KM_INFO, altitude / 1000.));
                speedSatelliteTwo.setText(String.format("%.2f Km/s", speed / 1000.));
            }
        });
    }

    public void setCollisionText(String satelliteName) {
        if (satelliteName.equals(satelliteOneLabel.getText())) {
            satelliteOneLabel.setText(UNKNOWN);
            latitudeSatelliteOne.setText(UNKNOWN);
            longitudeSatelliteOne.setText(UNKNOWN);
            satelliteOneAltitude.setText(UNKNOWN);
            speedSatelliteOne.setText(UNKNOWN);
        } else if (satelliteName.equals(satelliteTwoLabel.getText())) {
            satelliteTwoLabel.setText(UNKNOWN);
            latitudeSatelliteTwo.setText(UNKNOWN);
            longitudeSatelliteTwo.setText(UNKNOWN);
            satelliteTwoAltitude.setText(UNKNOWN);
            speedSatelliteTwo.setText(UNKNOWN);
        }
    }

    public void clearSatellitesDataFromFields() {
        clearSatellitesStates();

        pcTextArea.setText("");
        closeApproachDateTextArea.setText("");
        closeApproachDistanceTextArea.setText("");
        startDateTextArea.setText("");
        endDateTextArea.setText("");


    }
    public void clearSatellitesStates() {
        latitudeSatelliteOne.setText("");
        longitudeSatelliteOne.setText("");
        satelliteOneAltitude.setText("");
        speedSatelliteOne.setText("");

        latitudeSatelliteTwo.setText("");
        longitudeSatelliteTwo.setText("");
        satelliteTwoAltitude.setText("");
        speedSatelliteTwo.setText("");
    }

    public SatelliteInformationUpdate getSatelliteUpdateCallback() {
        return this;
    }

}
