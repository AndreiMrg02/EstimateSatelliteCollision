package com.ucv.util;

import com.ucv.controller.SatelliteController;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class CustomAlert {

    public void alertNoResults(SatelliteController satelliteController, BorderPane mainPanel) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "No results were found for the current settings", ButtonType.OK);
        satelliteController.getSatelliteTable().refresh();
        alert.showAndWait();
        LoggerCustom.getInstance().logMessage("INFO: Change configuration to find data");
        mainPanel.setDisable(false);
    }

    public void alertInvalidCredentials() {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Authentication Failed");
        alert.setHeaderText("Invalid Credentials");
        alert.setContentText("The username or password is incorrect." + "\nIMPORTANT: The application will close. " + "Reopen the application and enter the login data correctly.\n");
        alert.showAndWait();
        System.exit(0);
    }

    public void alertWaitSpaceTrackTle(StackPane informationPane) {
        informationPane.setVisible(false);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Extract error");
        alert.setHeaderText("Space-Track TLE Unavailable");
        alert.setContentText("To use the data download method using Space-Track TLE, you must wait one hour after the last use of it. " + "\nTo run the application you can use the Local TLE option using the TLEs generated file at the last run of this option.\n");
        alert.showAndWait();
    }
    public void radioButtonAlert(SatelliteController satelliteController) {

        Alert alert = new Alert(Alert.AlertType.ERROR, "Please choose the type of data downloading(Local Tle or Space-Track Tle)", ButtonType.OK);
        satelliteController.getSatelliteTable().refresh();
        alert.showAndWait();
        LoggerCustom.getInstance().logMessage("INFO: Choose Local Tle or Space-Track Tle");
        LoggerCustom.getInstance().logMessage("IMPORTANT: The call to Space-Track can only be made once per hour!");
    }

}
