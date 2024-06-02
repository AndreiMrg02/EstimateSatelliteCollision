package com.ucv.controller;

import com.ucv.util.PaneCustomStyle;
import com.ucv.datamodel.satellite.CollisionData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class SatelliteInformationController implements Initializable, SatelliteUpdateCallback  {
    @FXML
    private  StackPane paneInformationCollision;
    @FXML
    private  TextArea startDateTextArea;
    @FXML
    private  TextArea closeApproachDateTextArea;
    @FXML
    private  TextArea endDateTextArea;
    @FXML
    private  TextArea closeApproachDistanceTextArea;
    @FXML
    private  TextArea thresholdTextArea;
    @FXML
    private  TextArea pcTextArea;
    @FXML
    private StackPane satelliteOnePane;
    @FXML
    private  Label satelliteOneLabel;
    @FXML
    private  TextArea satelliteOneAltitude;
    @FXML
    private  TextArea longitudeSatelliteOne;
    @FXML
    private  TextArea latitudeSatelliteOne;
    @FXML
    private  TextArea speedSatelliteOne;
    @FXML
    private  StackPane satelliteTwoPane;
    @FXML
    private  Label satelliteTwoLabel;
    @FXML
    private  TextArea satelliteTwoAltitude;
    @FXML
    private  TextArea longitudeSatelliteTwo;
    @FXML
    private  TextArea latitudeSatelliteTwo;
    @FXML
    private  TextArea speedSatelliteTwo;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roundedPane();
    }
    public void roundedPane(){
        PaneCustomStyle paneCustomStyle = new PaneCustomStyle();
        paneCustomStyle.addClip(paneInformationCollision, 540, 256, 30, 30);
        paneCustomStyle.addClip(satelliteOnePane, 30, 30);
        paneCustomStyle.addClip(satelliteTwoPane, 30, 30);
    }

    public void setTextArea(CollisionData newValue, String thresholdValue) {

            if (newValue != null) {
                pcTextArea.setText(newValue.getCollisionProbability());
                startDateTextArea.setText(newValue.getStartDate());
                endDateTextArea.setText(newValue.getEndDate());
                closeApproachDateTextArea.setText(newValue.getCloseApproachDate());
                closeApproachDistanceTextArea.setText(newValue.getCloseApproach());
                satelliteOneLabel.setText(newValue.getSat1Name());
                satelliteTwoLabel.setText(newValue.getSat2Name());
                thresholdTextArea.setText(thresholdValue);
            }

    }
    @Override
    public void updateSatelliteData(String satelliteName, double latitude, double longitude, double altitude, double speed) {
        Platform.runLater(() -> {
            if (satelliteName.equals(satelliteOneLabel.getText())) {
                latitudeSatelliteOne.setText(String.valueOf(Math.toDegrees(latitude)));
                longitudeSatelliteOne.setText(String.valueOf(Math.toDegrees(longitude)));
                satelliteOneAltitude.setText(String.valueOf(altitude));
                speedSatelliteOne.setText(String.valueOf(speed));
            } else if (satelliteName.equals(satelliteTwoLabel.getText())) {
                latitudeSatelliteTwo.setText(String.valueOf(Math.toDegrees(latitude)));
                longitudeSatelliteTwo.setText(String.valueOf(Math.toDegrees(longitude)));
                satelliteTwoAltitude.setText(String.valueOf(altitude));
                speedSatelliteTwo.setText(String.valueOf(speed));
            }
        });
    }

    public void clearSatellitesDataFromFields() {
        latitudeSatelliteOne.setText("");
        longitudeSatelliteOne.setText("");
        satelliteOneAltitude.setText("");
        speedSatelliteOne.setText("");

        latitudeSatelliteTwo.setText("");
        longitudeSatelliteTwo.setText("");
        satelliteTwoAltitude.setText("");
        speedSatelliteTwo.setText("");

    }
    public SatelliteUpdateCallback getSatelliteUpdateCallback(){
        return this;
    }


}
