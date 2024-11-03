package com.ucv.controller;

import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.util.UtilConstant;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private static final Logger logger = LogManager.getLogger(LoginController.class);
    @FXML
    private PasswordField pwBox;
    @FXML
    private TextField userTextField;
    private boolean authenticated;
    private InternetConnectionData internetConnectionData;

    private boolean validateEmail(String email) {
        return email != null && !email.trim().isEmpty();
    }

    private boolean validatePassword(String password) {
        return password != null && !password.trim().isEmpty();
    }

    public void connect() {
        if (!validateEmail(userTextField.getText())) {
            showAlert("Invalid Email", "Please enter your email address.");
            return;
        }

        if (!validatePassword(pwBox.getText())) {
            showAlert("Invalid Password", "Please enter your password.");
            return;
        }

        try {
            String baseURL = "https://www.space-track.org";
            String authPath = UtilConstant.URL_SPACE_TRACK;
            authenticated = true;
            internetConnectionData = new InternetConnectionData(baseURL, authPath, userTextField.getText(), pwBox.getText());
            Stage stage = (Stage) userTextField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            logger.error("Error during connection attempt", e);
            authenticated = false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void handleClose() {
        System.exit(0);
    }

    public void handleSignIn() {
        connect();
    }

    public void handleRegister() {
        try {
            String url = "https://www.space-track.org/auth/createAccount";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Communication Failed");
                alert.setHeaderText("Invalid browser");
                alert.setContentText("Browser doesn't support this action.");
                alert.showAndWait();
            }
        } catch (Exception e) {
            logger.error("Error during register", e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authenticated = false;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public InternetConnectionData getInternetConnectionData() {
        return internetConnectionData;
    }
}
