package com.ucv.util;

import com.ucv.controller.SatelliteController;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldValidator {
    private static final String REGEX = "^\\d+$";
    private final RadioButton spaceTrackTleRadio;
    private final RadioButton localTleRadio;
    private final TextArea valueField;
    private final TextArea thresholdBox;
    private final SatelliteController satelliteController;

    public FieldValidator(RadioButton spaceTrackTleRadio, RadioButton localTleRadio, TextArea valueField,
                          TextArea thresholdBox, SatelliteController satelliteController) {
        this.spaceTrackTleRadio = spaceTrackTleRadio;
        this.localTleRadio = localTleRadio;
        this.valueField = valueField;
        this.thresholdBox = thresholdBox;
        this.satelliteController = satelliteController;
    }

    public boolean validateInputs() {
        CustomAlert customAlert = new CustomAlert();
        if (!validateValueField() || !validateThresholdField()) {
            return false;
        }
        if (!spaceTrackTleRadio.isSelected() && !localTleRadio.isSelected()) {
            customAlert.radioButtonAlert(satelliteController);
            return false;
        }
        return true;
    }

    public boolean validateThresholdField() {

        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(thresholdBox.getText());
        if (matcher.matches()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "The threshold field must be a digit or a number!", ButtonType.OK);
            alert.showAndWait();
            return false;
        }
    }

    public boolean validateValueField() {

        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(valueField.getText());
        if (matcher.matches()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "The value field must be a digit or a number!", ButtonType.OK);
            alert.showAndWait();
            return false;
        }
    }
}
