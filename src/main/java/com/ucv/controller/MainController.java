package com.ucv.controller;

import com.ucv.Main;
import com.ucv.database.DBOperation;
import com.ucv.datamodel.database.ConnectionInformation;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import org.apache.log4j.Logger;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ucv.database.DBOperation.clearAllStates;
import static com.ucv.database.DBOperation.getLastConnectionFromDB;
import static com.ucv.database.HibernateUtil.closeSession;

public class MainController implements Initializable {

    private static final Logger logger = Logger.getLogger(MainController.class);
    private static final String REGEX = "^[0-9]+$";
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
    private ObservableList<String> operatorList;
    private EarthViewController earthViewController;
    private SatelliteController satelliteController;
    private SatelliteInformationController satelliteInformationController;
    private InternetConnectionData connectionData;
    private double xOffset = 0;
    private double yOffset = 0;
    private Task<Void> currentTask;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        operatorList = FXCollections.observableArrayList();
        addOperatorToList();

        operatorBox.setItems(operatorList);
        progressBar.setProgress(-1.0);
        progressBar.setVisible(false);
        setButtonStyle();
        manageRadioButtons();
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
        menuPanel.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        menuPanel.setOnMouseDragged(event -> {
            mainPanel.getScene().getWindow().setX(event.getScreenX() - xOffset);
            mainPanel.getScene().getWindow().setY(event.getScreenY() - yOffset);
        });

    }

    public void manageRadioButtons() {
        ToggleGroup group = new ToggleGroup();
        group.getToggles().add(localTleRadio);
        group.getToggles().add(spaceTrackTleRadio);
    }

    public void loadSatelliteInformation() {
        try {
            FXMLLoader fxmlLoaderInformation = new FXMLLoader(getClass().getResource("/views/SatelliteInformation.fxml"));
            StackPane paneWithTable = fxmlLoaderInformation.load();
            satelliteInformationController = fxmlLoaderInformation.getController();
            informationPane.getChildren().add(paneWithTable);
            informationPane.setVisible(false);
        } catch (Exception ex) {
            logger.error("Failed to load the satellite information view.", ex);
        }
    }

    public void loadEarth() {
        try {
            FXMLLoader fxmlLoaderEarth = new FXMLLoader(getClass().getResource("/views/EarthViewNou.fxml"));
            StackPane paneWithEarth = fxmlLoaderEarth.load();
            earthViewController = fxmlLoaderEarth.getController();
            Platform.runLater(() -> earthPane.getChildren().add(paneWithEarth));
        } catch (Exception ex) {
            logger.error("Failed to load the Earth view.", ex);
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
            logger.error("Failed to load the satellite view.", e);
        }
        processSatelliteData();
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
            closeButton.setDisable(true);
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
            closeButton.setDisable(false);
            LoggerCustom.getInstance().logMessage("INFO: The simulation was stopped");
            earthViewController.delete();
            showSatellitesButton.setDisable(false);
            simulateCollision.setDisable(true);
            pauseButton.setDisable(true);
            closeApproachButton.setDisable(true);
            stopSimulationButton.setDisable(true);
            satelliteInformationController.clearSatellitesDataFromFields();
            resumeButton.setDisable(true);
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
            resumeButton.setDisable(true);
            pauseButton.setDisable(false);
            showSatellitesButton.setDisable(true);
        });

        simulateCollision.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: A collision was simulated");

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
            if (!spaceTrackTleRadio.isSelected() && !localTleRadio.isSelected()) {
                radioButtonAlert();
                return;
            }
            Map<String, String[]> tleData = new LinkedHashMap<>();
            if (localTleRadio.isSelected()) {
                tleData = chooseTleFile();
                if (tleData.isEmpty()) {
                    LoggerCustom.getInstance().logMessage("WARNING: TLEs file is empty or you don't choose a file");
                    LoggerCustom.getInstance().logMessage("WARNING: Please verify if you choose a file that contain only TLEs");
                    return;
                }
            }
            String operator = setOperator(operatorBox.getValue());
            resetDataForNewExtraction();
            Map<String, String[]> finalTleData = tleData;

            currentTask = new Task<>() {
                @Override
                protected Void call() {
                    LoggerCustom.getInstance().logMessage("INFO: The process for extract the data is running...");
                    Platform.runLater(() -> {
                        mainPanel.setDisable(true);
                        menuPanel.setDisable(false);
                        displayProgressBar();
                    });

                    CollectSatelliteData collectSatelliteData = new CollectSatelliteData(connectionData);
                    Map<String, Item> listOfUniqueSatelliteTemp = collectSatelliteData.extractSatelliteData("MIN_RNG", operator, valueField.getText());

                    if (listOfUniqueSatelliteTemp != null) {
                        satelliteController.setListOfUniqueSatellite(listOfUniqueSatelliteTemp);
                        if (downloadTLEs(listOfUniqueSatelliteTemp, finalTleData, collectSatelliteData)) {
                            Platform.runLater(() -> {
                                progressBar.setVisible(false);
                                if (listOfUniqueSatelliteTemp.isEmpty()) {
                                    alertNoResults();
                                    event.consume();
                                }
                                setTaskOnSuccess();
                            });
                        } else {
                            cancelTask();
                            return null;
                        }
                    } else {
                        Platform.runLater(() -> {
                            alertInvalidCredentials();
                            event.consume();
                        });
                        return null;
                    }
                    return null;
                }
            };
            setTaskOnFailed(currentTask);
            new Thread(currentTask).start();
        });
    }

    private void cancelTask() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel(true);
            LoggerCustom.getInstance().logMessage("INFO: Task was cancelled.");
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                mainPanel.setDisable(false);
                showSatellitesButton.setDisable(true);
            });
        }
    }

    private void setTaskOnFailed(Task<Void> task) {
        task.setOnFailed(e -> Platform.runLater(() -> {
            mainPanel.setDisable(false);
            progressBar.setVisible(false);
            showSatellitesButton.setDisable(true);
        }));
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
        alert.setContentText("The username or password is incorrect." + "\nIMPORTANT: The application will close. " + "Reopen the application and enter the login data correctly.\n");
        alert.showAndWait();
        System.exit(0);
    }

    public void alertWaitSpaceTrackTle() {
        informationPane.setVisible(false);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Extract error");
        alert.setHeaderText("Space-Track TLE Unavailable");
        alert.setContentText("To use the data download method using Space-Track TLE, you must wait one hour after the last use of it. " + "\nTo run the application you can use the Local TLE option using the TLEs generated file at the last run of this option.\n");
        alert.showAndWait();
    }

    public void setTaskOnSuccess() {
        mainPanel.setDisable(false);
        satelliteController.getSatelliteTable().refresh();
        informationPane.setVisible(true);
        updateCollisionInformation();
        LoggerCustom.getInstance().logMessage("INFO: Satellite's data were downloaded.");
    }

    private void successExtractList(BorderPane tableViewLayout) {
        progressBar.setVisible(false);
        tableViewPane.setVisible(true);
        tableViewPane.setCenter(tableViewLayout);
    }

    public void radioButtonAlert() {

        Alert alert = new Alert(Alert.AlertType.ERROR, "Please choose the type of data downloading(Local Tle or Space-Track Tle)", ButtonType.OK);
        satelliteController.getSatelliteTable().refresh();
        alert.showAndWait();
        LoggerCustom.getInstance().logMessage("INFO: Choose Local Tle or Space-Track Tle");
        LoggerCustom.getInstance().logMessage("IMPORTANT: The call to Space-Track can only be made once per hour!");
    }

    private boolean downloadTLEs(Map<String, Item> listOfUniqueSatellite, Map<String, String[]> tleData, CollectSatelliteData collectSatelliteData) {
        if (localTleRadio.isSelected()) {
            extractTLEsUsingLocalFile(listOfUniqueSatellite, tleData);
            return true;
        } else if (spaceTrackTleRadio.isSelected()) {
            if (hasOneHourPassedSinceLastConnection(connectionData.getUserName())) {
                extractTLEsUsingSpaceTrack(listOfUniqueSatellite, collectSatelliteData);
                return true;
            } else {
                Platform.runLater(this::alertWaitSpaceTrackTle);
                return false;
            }
        }
        return false;
    }

    public Map<String, String[]> chooseTleFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select TLE File");
        Map<String, String[]> tleData = new LinkedHashMap<>();
        File tleFile = fileChooser.showOpenDialog(mainPanel.getScene().getWindow());
        if (tleFile != null) {
            tleData = readTLEFile(tleFile.getAbsolutePath());
        } else {
            logger.error("File selection cancelled by user.");
        }
        return tleData;
    }

    private void extractTLEsUsingLocalFile(Map<String, Item> listOfUniqueSatellite, Map<String, String[]> tleData) {

        for (Item item : listOfUniqueSatellite.values()) {
            if (tleData.containsKey(item.getSat1Id())) {
                String[] tle = tleData.get(item.getSat1Id());
                String tleString = tle[0] + "\n" + tle[1];
                satelliteController.addSpatialObject(item.getTca(), item.getSat1Name(), tleString);
            }
            if (tleData.containsKey(item.getSat2Id())) {
                String[] tle = tleData.get(item.getSat2Id());
                String tleString = tle[0] + "\n" + tle[1];
                satelliteController.addSpatialObject(item.getTca(), item.getSat2Name(), tleString);
            }
        }
        satelliteController.manageSatellites();
    }

    public Map<String, String[]> readTLEFile(String filePath) {
        Map<String, String[]> tleMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("1 ")) {
                    String line2 = reader.readLine();
                    if (line2 != null && line2.startsWith("2 ")) {
                        String satelliteId = line.substring(2, 7).trim();
                        String[] tle = {line, line2};
                        tleMap.put(satelliteId, tle);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("The TLE file contains corrupt data", e);
        }
        return tleMap;
    }

    private void extractTLEsUsingSpaceTrack(Map<String, Item> listOfUniqueSatellite, CollectSatelliteData collectSatelliteData) {

        ConnectionInformation connectionInformation = generateConnectionInfo();
        DBOperation.saveLastConnectionToDB(connectionInformation);
        String queryToDownloadAllTLEs = "/basicspacedata/query/class/gp/decay_date/null-val/epoch/%3Enow-30/orderby/norad_cat_id/format/tle";
        String allTles = collectSatelliteData.extractSatelliteTLEs(queryToDownloadAllTLEs);
        String[] lines = allTles.split("\n");
        Map<String, String> satelliteTLEs = new LinkedHashMap<>();
        for (int i = 0; i < lines.length; i += 2) {
            if (i + 1 < lines.length) {
                String line1 = lines[i];
                String line2 = lines[i + 1];
                String satelliteId = line1.substring(2, 7).trim();
                String tle = line1 + "\n" + line2;
                satelliteTLEs.put(satelliteId, tle);
            }
        }
        for (Item item : listOfUniqueSatellite.values()) {
            if (satelliteTLEs.containsKey(item.getSat1Id())) {
                String tle = satelliteTLEs.get(item.getSat1Id());
                satelliteController.addSpatialObject(item.getTca(), item.getSat1Name(), tle);
            }
            if (satelliteTLEs.containsKey(item.getSat2Id())) {
                String tle = satelliteTLEs.get(item.getSat2Id());
                satelliteController.addSpatialObject(item.getTca(), item.getSat2Name(), tle);
            }
        }
        generateTleFile(satelliteTLEs);
        satelliteController.manageSatellites();
    }

    private ConnectionInformation generateConnectionInfo() {
        ConnectionInformation connectionInformation = new ConnectionInformation();
        connectionInformation.setUsername(connectionData.getUserName());
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        connectionInformation.setLastConnectionDate(formattedDateTime);
        return connectionInformation;
    }

    public boolean hasOneHourPassedSinceLastConnection(String username) {
        ConnectionInformation connectionInformation = getLastConnectionFromDB(username);
        if (connectionInformation == null) {
            return true;
        }
        String lastConnectionDateStr = connectionInformation.getLastConnectionDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime lastConnectionDate = LocalDateTime.parse(lastConnectionDateStr, formatter);
        LocalDateTime currentDate = LocalDateTime.now();

        Duration duration = Duration.between(lastConnectionDate, currentDate);

        if (duration.toHours() >= 1) {
            return true;
        } else {
            long minutesLeft = 60 - duration.toMinutes();
            long secondsLeft = 3600 - duration.getSeconds();
            LoggerCustom.getInstance().logMessage(String.format("IMPORTANT:The remaining time to use this option again is: %s min and %s sec", minutesLeft, (secondsLeft % 60)));
            return false;
        }
    }

    public void generateTleFile(Map<String, String> satelliteTLEs) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "tle_output_" + timestamp + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, String> entry : satelliteTLEs.entrySet()) {
                writer.write(entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("Unexpected exception occurred due to generate tle output file: " + e.getMessage());
        }
        LoggerCustom.getInstance().logMessage("The application generate a .txt file with the most recently TLE with name: " + fileName);
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

    public void setConnectionData(InternetConnectionData connectionData) {
        this.connectionData = connectionData;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public Button getMinimizeButton() {
        return minimizeButton;
    }

}