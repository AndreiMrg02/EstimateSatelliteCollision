package com.ucv.controller;

import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.handler.SatelliteTaskHandler;
import com.ucv.helper.LoaderFXML;
import com.ucv.helper.MainControllerAction;
import com.ucv.implementation.ConnectionService;
import com.ucv.implementation.DisplaySatelliteManager;
import com.ucv.tle.TleService;
import com.ucv.util.ButtonCustomStyle;
import com.ucv.util.FieldValidator;
import com.ucv.util.LoggerCustom;
import com.ucv.util.PaneCustomStyle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.*;

import static com.ucv.database.DBOperation.clearAllStates;
import static com.ucv.database.HibernateUtil.closeSession;

public class MainController implements Initializable {

    @FXML
    private ChoiceBox<String> calculateMethodBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private TextArea noOfDaysBox;
    @FXML
    private RadioButton spaceTrackTleRadio;
    @FXML
    private RadioButton localTleRadio;
    @FXML
    private HBox menuPanel;
    @FXML
    private Button minimizeButton;
    @FXML
    private Button closeButton;
    @FXML
    private StackPane informationPane;
    @FXML
    private ScrollPane scrollPaneLog;
    @FXML
    private TextArea thresholdBox;
    @FXML
    private TextFlow loggerBox;
    @FXML
    private VBox configurationPane;
    @FXML
    private BorderPane mainPanel;
    @FXML
    private Button stopSimulationButton;
    @FXML
    private Button simulateCollision;
    @FXML
    private Button resumeButton;
    @FXML
    private Button closeApproachButton;
    @FXML
    private Button pauseButton;
    @FXML
    private Button showSatellitesButton;
    @FXML
    private StackPane earthPane;
    @FXML
    private BorderPane tableViewPane;
    @FXML
    private ProgressIndicator progressBar;
    @FXML
    private Button extractDataButton;
    @FXML
    private ChoiceBox<String> operatorBox;
    @FXML
    private TextArea valueField;

    private EarthViewController earthViewController;
    private SatelliteController satelliteController;
    private SatelliteInformationController satelliteInformationController;
    private TleService tleService;
    private InternetConnectionData connectionData;
    private ConnectionService connectionService;
    private double xOffset = 0;
    private double yOffset = 0;
    private Task<Void> currentTask;
    private FieldValidator fieldValidator;
    private DisplaySatelliteManager displaySatelliteManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<String> operatorList;
        ObservableList<String> methodList;
        startDatePicker.setDisable(true);
        endDatePicker.setDisable(true);
        noOfDaysBox.setDisable(true);
        LoaderFXML fxmlLoader = new LoaderFXML(tableViewPane);
        methodList = FXCollections.observableArrayList("No. days", "Date");
        operatorList = FXCollections.observableArrayList("=", "<", ">");
        operatorBox.setItems(operatorList);
        calculateMethodBox.setItems(methodList);
        progressBar.setProgress(-1.0);
        progressBar.setVisible(false);
        setButtonStyle();
        manageRadioButtons();
        satelliteController = fxmlLoader.loadTableSatellite(mainPanel, progressBar);
        processSatelliteData();
        closeSession();
        roundedPane();
        scrollPaneLog.setFitToWidth(true);
        fieldValidator = new FieldValidator(spaceTrackTleRadio, localTleRadio, valueField, thresholdBox, satelliteController);
        scrollPaneLog.setContent(loggerBox);
        LoggerCustom.getInstance().setConsole(loggerBox, scrollPaneLog);
        satelliteInformationController = fxmlLoader.loadSatelliteInformation(informationPane);
        earthViewController = fxmlLoader.loadEarth(earthPane);
        earthViewController.setUpdateSatellitesInformation(satelliteInformationController.getSatelliteUpdateCallback());
        displaySatelliteManager = new DisplaySatelliteManager(satelliteController, earthViewController);
        displaySatelliteManager.drawEarthAfterInit();
        buttonSettings();
        menuPanel.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        menuPanel.setOnMouseDragged(event -> {
            mainPanel.getScene().getWindow().setX(event.getScreenX() - xOffset);
            mainPanel.getScene().getWindow().setY(event.getScreenY() - yOffset);
        });
        calculateMethodBox.valueProperty().addListener((observableValue, s, t1) -> {
            if (calculateMethodBox.getSelectionModel().getSelectedItem().equals("No. days")) {
                startDatePicker.setDisable(true);
                endDatePicker.setDisable(true);
                noOfDaysBox.setDisable(false);
            } else {
                startDatePicker.setDisable(false);
                endDatePicker.setDisable(false);
                noOfDaysBox.setDisable(true);
                noOfDaysBox.setText("0");
            }
        });
    }

    public void manageRadioButtons() {
        ToggleGroup group = new ToggleGroup();
        group.getToggles().add(localTleRadio);
        group.getToggles().add(spaceTrackTleRadio);
    }

    private void setButtonStyle() {
        ButtonCustomStyle customStyle = new ButtonCustomStyle();
        customStyle.setButtonStyle(showSatellitesButton);
        customStyle.setButtonStyle(pauseButton);
        customStyle.setButtonStyle(closeApproachButton);
        customStyle.setButtonStyle(resumeButton);
        customStyle.setButtonStyle(stopSimulationButton);
        customStyle.setButtonStyle(simulateCollision);
    }

    public void roundedPane() {
        PaneCustomStyle paneCustomStyle = new PaneCustomStyle();
        paneCustomStyle.addClip(earthPane, 30, 30);
        paneCustomStyle.addClip(configurationPane, 30, 30);
        paneCustomStyle.addClip(loggerBox, 15, 15);
        paneCustomStyle.addClip(scrollPaneLog, 16, 16);
    }


    private void buttonSettings() {
        stopSimulationButton.setDisable(true);
        resumeButton.setDisable(true);
        MainControllerAction buttonController = new MainControllerAction(stopSimulationButton, simulateCollision, resumeButton, closeApproachButton, pauseButton, showSatellitesButton, extractDataButton);
        buttonController.showSatellitesAction(displaySatelliteManager, satelliteController);
        buttonController.stopSimulationAction(closeButton, earthViewController, satelliteInformationController, satelliteController);
        buttonController.pauseAction(earthViewController);
        buttonController.closeApproachAction(earthViewController);
        buttonController.simulateCollisionAction(earthViewController, satelliteInformationController);
    }

    private void processSatelliteData() {

        extractDataButton.setOnMouseClicked(event -> {
            if (!fieldValidator.validateInputs()) {
                return;
            }
            connectionService = new ConnectionService(connectionData);
            int days = Integer.parseInt(noOfDaysBox.getText());
            tleService = new TleService(connectionService, satelliteController, localTleRadio, days, startDatePicker.getValue(), endDatePicker.getValue());
            Map<String, String[]> tleData = new HashMap<>();
            if (localTleRadio.isSelected()) {
                tleData = tleService.getTleData(mainPanel);
                if (tleData == null || tleData.isEmpty()) {
                    return;
                }
            }
            String operator = setOperator(operatorBox.getValue());
            resetDataForNewExtraction();
            SatelliteTaskHandler satelliteTaskHandler = new SatelliteTaskHandler(tleService, connectionData, satelliteController, satelliteInformationController, progressBar, mainPanel, thresholdBox.getText());
            currentTask = satelliteTaskHandler.createSatelliteDataTask(tleData, operator, event, valueField, spaceTrackTleRadio, informationPane, showSatellitesButton);
            setTaskOnFailed(currentTask);
            new Thread(currentTask).start();
        });
    }


    private void setTaskOnFailed(Task<Void> task) {
        task.setOnFailed(e -> Platform.runLater(() -> {
            mainPanel.setDisable(false);
            progressBar.setVisible(false);
            showSatellitesButton.setDisable(true);
        }));
    }


    public void resetDataForNewExtraction() {
        clearAllStates();
        satelliteInformationController.clearSatellitesDataFromFields();
        satelliteController.setThreshold(Integer.parseInt(thresholdBox.getText()));
        satelliteController.getSatelliteTable().getItems().clear();
        satelliteController.setTwoSatellitesSelected(new ArrayList<>());
        satelliteController.setDisplaySatelliteModels(new HashSet<>());
    }


    private String setOperator(String operator) {
        switch (operator) {
            case "=":
                operator = "";
                break;
            case "<":
                operator = "%3C";
                break;
            case ">":
                operator = "%3E";
                break;
            default:
                operator = "%3C%3E";
                break;
        }
        return operator;
    }

    public synchronized void setConnectionData(InternetConnectionData connectionData) {
        this.connectionData = connectionData;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public Button getMinimizeButton() {
        return minimizeButton;
    }

}