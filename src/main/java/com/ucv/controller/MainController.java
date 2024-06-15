package com.ucv.controller;

import com.ucv.Main;
import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import com.ucv.util.ButtonCustomStyle;
import com.ucv.util.LoggerCustom;
import com.ucv.util.PaneCustomStyle;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ucv.database.DBOperation.clearAllStates;
import static com.ucv.database.HibernateUtil.closeSession;

public class MainController implements Initializable {

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
    Button resumeButton;
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
    private ObservableList<String> operatorList;
    private EarthViewController earthViewController;
    private SatelliteController satelliteController;
    private SatelliteInformationController satelliteInformationController;
    private InternetConnectionData connectionData;

    private static final String regex = "^[0-9]+$";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        operatorList = FXCollections.observableArrayList();
        addOperatorToList();

        operatorBox.setItems(operatorList);
        progressBar.setProgress(-1.0);
        progressBar.setVisible(false);
        setButtonStyle();
        loadTableSatellite();
        closeSession();
        buttonSettings();
        roundedPane();
        loadEarth();
        drawEarthAfterInit();
        scrollPaneLog.setFitToWidth(true);
        scrollPaneLog.setContent(loggerBox);
        LoggerCustom.getInstance().setConsole(loggerBox, scrollPaneLog);
        loadSatelliteInformation();
        earthViewController.setUpdateSatellitesInformation(satelliteInformationController.getSatelliteUpdateCallback());

    }

    public void loadSatelliteInformation() {
        try {
            FXMLLoader fxmlLoaderInformation = new FXMLLoader(getClass().getResource("/views/SatelliteInformation.fxml"));
            StackPane paneWithTable = fxmlLoaderInformation.load();
            satelliteInformationController = fxmlLoaderInformation.getController();
            informationPane.getChildren().add(paneWithTable);
            informationPane.setVisible(false);
        } catch (Exception ex) {
            System.out.println("An exception occurred due to can not instantiate the satellite information pane.");
        }
    }

    public void loadEarth() {
        try {
            FXMLLoader fxmlLoaderEarth = new FXMLLoader(getClass().getResource("/views/EarthViewNou.fxml"));
            StackPane paneWithEarth = fxmlLoaderEarth.load();
            earthViewController = fxmlLoaderEarth.getController();
            earthPane.getChildren().add(paneWithEarth);
        } catch (Exception ex) {
            System.out.println("An exception occurred due to can not instantiate the earth pane.");
            ex.printStackTrace();
        }
    }

    public void loadTableSatellite() {
        try {
            tableViewPane = (BorderPane) mainPanel.lookup("#tableViewPane");
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/TableSatelliteExtended.fxml"));
            BorderPane tableViewLayout = fxmlLoader.load();
            satelliteController = fxmlLoader.getController();
            successExtractList(tableViewLayout);
        } catch (IOException e) {
            e.printStackTrace();
        }
        processSatelliteData();
    }

    public boolean validateThresholdField() {

        Pattern pattern = Pattern.compile(regex);
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

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(valueField.getText());
        if (matcher.matches()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "The value field must be a digit or a number!", ButtonType.OK);
            alert.showAndWait();
            return false;
        }
    }

    private void drawEarthAfterInit() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                EarthViewController.wwd.redraw();
            }
        };
        timer.start();
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


    private void addOperatorToList() {
        operatorList.add("=");
        operatorList.add("<");
        operatorList.add(">");
    }

    private void buttonSettings() {
        stopSimulationButton.setDisable(true);
        resumeButton.setDisable(true);
        showSatellitesButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: Check the map to see the satellites");
            displaySatellites();
            simulateCollision.setDisable(false);
            pauseButton.setDisable(false);
            stopSimulationButton.setDisable(false);
            closeApproachButton.setDisable(false);
            showSatellitesButton.setDisable(true);
            satelliteController.getSatelliteTable().setDisable(true);
        });

        stopSimulationButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: The simulation was stopped");
            earthViewController.delete();
            showSatellitesButton.setDisable(false);
            simulateCollision.setDisable(true);
            pauseButton.setDisable(true);
            closeApproachButton.setDisable(true);
            stopSimulationButton.setDisable(true);
            extractDataButton.setDisable(false);
            earthViewController.triggerCollision(false);
            satelliteController.getSatelliteTable().setDisable(false);
        });

        pauseButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: Simulation paused");
            earthViewController.pauseSimulation();
            resumeButton.setDisable(false);
            pauseButton.setDisable(true);
        });

        closeApproachButton.setOnAction(event -> showSatellitesAtCloseApproach());
        resumeButton.setOnAction(event -> {
            earthViewController.resumeSimulation();
            pauseButton.setDisable(false);
            showSatellitesButton.setDisable(true);
        });

        simulateCollision.setOnAction(event -> {
            showSatellitesAtCloseApproach();
            pauseButton.setDisable(false);
            resumeButton.setDisable(true);
            satelliteInformationController.clearSatellitesDataFromFields();
            earthViewController.triggerCollision(true);
            earthViewController.pauseSimulation();
            earthViewController.resumeSimulation();
        });
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

    public void showSatellitesAtCloseApproach() {
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

    private void processSatelliteData() {
        extractDataButton.setOnMouseClicked(event -> {
            if (!validateValueField() || !validateThresholdField()) {
                return;
            }
            String operator = setOperator(operatorBox.getValue());
            resetDataForNewExtraction();
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    LoggerCustom.getInstance().logMessage("INFO: The process for extract the data is running...");
                    mainPanel.setDisable(true);
                    displayProgressBar();
                    CollectSatelliteData collectSatelliteData = new CollectSatelliteData(connectionData);
                    Map<String, Item> listOfUniqueSatelliteTemp = collectSatelliteData.extractSatelliteData("MIN_RNG", operator, valueField.getText());
                    if(listOfUniqueSatelliteTemp != null){
                        satelliteController.setListOfUniqueSatellite(listOfUniqueSatelliteTemp);
                        downloadTLEs(collectSatelliteData, listOfUniqueSatelliteTemp);
                    }
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        if(listOfUniqueSatelliteTemp == null){
                           alertInvalidCredentials();
                           event.consume();
                        }
                        if (listOfUniqueSatelliteTemp != null && listOfUniqueSatelliteTemp.isEmpty()) {
                            alertNoResults();
                            event.consume();
                        }
                        setTaskOnSuccess();
                    });
                    return null;
                }
            };
            setTaskOnFailed(task);
            new Thread(task).start();
        });
    }

    private void displayProgressBar() {
        progressBar.toFront();
        progressBar.setVisible(true);
        progressBar.setStyle("-fx-accent: #ff0000;"); // Change the color to red for better visibility
        progressBar.setScaleX(1.5);
        progressBar.setOpacity(1.0);
        progressBar.setScaleY(1.5);
    }

    public void resetDataForNewExtraction() {
        clearAllStates();
        satelliteInformationController.clearSatellitesDataFromFields();
        satelliteController.setThreshold(Integer.parseInt(thresholdBox.getText()));
        satelliteController.getSatelliteTable().getItems().clear();
        satelliteController.setTwoSatellitesSelected(new ArrayList<>());
        satelliteController.setDisplaySatelliteModels(new HashSet<>());
    }

    public void alertNoResults() {
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
        alert.setContentText("The username or password is incorrect." +
                "\nIMPORTANT: The application will close. " +
                "Reopen the application and enter the login data correctly.\n");
        alert.showAndWait();
        System.exit(0);
    }

    public void setTaskOnSuccess() {
        mainPanel.setDisable(false);
        satelliteController.getSatelliteTable().refresh();
        informationPane.setVisible(true);
        updateCollisionInformation();
        LoggerCustom.getInstance().logMessage("INFO: Satellite's data were downloaded.");
    }

    private void setTaskOnFailed(Task<Void> task) {
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            Platform.runLater(() -> {
                mainPanel.setDisable(false);
                progressBar.setVisible(false);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Unexpected error occurred: " + exception.getMessage(), ButtonType.OK);
                alert.showAndWait();
            });
        });
    }

    private void successExtractList(BorderPane tableViewLayout) {
        progressBar.setVisible(false);
        tableViewPane.setVisible(true);
        tableViewPane.setCenter(tableViewLayout);
    }

    private void downloadTLEs(CollectSatelliteData downloadTLE, Map<String, Item> listOfUniqueSatellite) {
        String specificTagBuilder = "%3C";
        String epochDesc = "%20";
        String queryFirstSatellite = "";
        String querySecondSatellite = "";
        String colonForSpaceTrack = "%3A";
        String newTca = "";
        for (Item item : listOfUniqueSatellite.values()) {
            if (item.getTca().contains(":")) {
                newTca = item.getTca().replace(":", colonForSpaceTrack);
            }

            queryFirstSatellite = String.format("/basicspacedata/query/class/tle/NORAD_CAT_ID/%s/EPOCH/%s%s/orderby/EPOCH%sdesc/limit/1/format/tle/emptyresult/show", item.getSat1Id(), specificTagBuilder, newTca, epochDesc);
            String tleSatellite = downloadTLE.extractSatelliteTLEs(queryFirstSatellite);
            if (!tleSatellite.isEmpty()) {
                satelliteController.addSpatialObject(item.getTca(), item.getSat1Name(), tleSatellite);
            }
            querySecondSatellite = String.format("/basicspacedata/query/class/tle/NORAD_CAT_ID/%s/EPOCH/%s%s/orderby/EPOCH%sdesc/limit/1/format/tle/emptyresult/show", item.getSat2Id(), specificTagBuilder, newTca, epochDesc);
            tleSatellite = downloadTLE.extractSatelliteTLEs(querySecondSatellite);
            if (!tleSatellite.isEmpty()) {
                satelliteController.addSpatialObject(item.getTca(), item.getSat2Name(), tleSatellite);
            }
        }
        satelliteController.manageSatellites();

    }

    private void updateCollisionInformation() {
        satelliteController.getSatelliteTable().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showSatellitesButton.setDisable(false);
                satelliteInformationController.setCollisionInformation(newValue, thresholdBox.getText());
            }
        });
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


    public InternetConnectionData getConnectionData() {
        return connectionData;
    }

    public void setConnectionData(InternetConnectionData connectionData) {
        this.connectionData = connectionData;
    }
}