package com.ucv.controller;

import com.ucv.Main;
import com.ucv.util.ButtonCustomStyle;
import com.ucv.util.LoggerCustom;
import com.ucv.util.PaneCustomStyle;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.io.IOException;
import java.net.URL;
import java.util.*;

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
    private Button showSatelliteButton;
    @FXML
    private StackPane earthPane;
    @FXML
    private BorderPane tableViewPane;
    @FXML
    private ProgressIndicator progressBar;
    @FXML
    private Button extractDataButton;
    @FXML
    private ChoiceBox<String> predicateBox;
    @FXML
    private ChoiceBox<String> operatorBox;
    @FXML
    private TextArea valueField;
    private EarthViewController earthViewController;
    private SatelliteController satelliteController;
    private SatelliteInformationController satelliteInformationController;

    public void loadFXML(Stage mainStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            BorderPane mainBorderPanel = fxmlLoader.load();
            Scene scene = new Scene(mainBorderPanel, 1484, 917);
            mainStage.setTitle("Satellite");
            tableViewPane = (BorderPane) mainBorderPanel.lookup("#tableViewPane");
            mainStage.setScene(scene);
            mainStage.show();

        } catch (Exception ex) {
            System.out.println("An exception occurred due to load the main stage");
            ex.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        ObservableList<String> predicateList = FXCollections.observableArrayList();
        ObservableList<String> operatorList = FXCollections.observableArrayList();
        addPredicateToList(predicateList);
        predicateBox.setItems(predicateList);

        addOperatorToList(operatorList);
        operatorBox.setItems(operatorList);
        progressBar.setProgress(-1.0);
        progressBar.setVisible(false);

        setButtonStyle();
        loadTableSatellite();
        closeSession();
        buttonFunction();
        roundedPane();
        loadEarth();
        drawEarthAfterInit();
        scrollPaneLog.setFitToWidth(true);
        scrollPaneLog.setContent(loggerBox);
        LoggerCustom.getInstance().setConsole(loggerBox, scrollPaneLog);
        loadSatelliteInformation();
        earthViewController.setCallback(satelliteInformationController.getSatelliteUpdateCallback());

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
        customStyle.setButtonStyle(showSatelliteButton);
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


    private void addPredicateToList(ObservableList<String> predicateList) {
        predicateList.add("MIN_RNG");
        predicateList.add("SAT_1_NAME");
    }

    private void addOperatorToList(ObservableList<String> operatorList) {
        operatorList.add("=");
        operatorList.add("<");
        operatorList.add(">");
    }

    private void buttonFunction() {
        resumeButton.setDisable(true);
        showSatelliteButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: Check the map to see the satellites");
            displaySatellites();
            pauseButton.setDisable(false);
            stopSimulationButton.setDisable(false);
            closeApproachButton.setDisable(false);
            showSatelliteButton.setDisable(true);
            satelliteController.getSatelliteTable().setDisable(true);
        });

        stopSimulationButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: The simulation was stopped");
            earthViewController.delete();
            satelliteInformationController.clearSatellitesDataFromFields();
            showSatelliteButton.setDisable(false);
            pauseButton.setDisable(true);
            closeApproachButton.setDisable(true);
            stopSimulationButton.setDisable(true);
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
            showSatelliteButton.setDisable(true);
        });

        simulateCollision.setOnAction(event -> {
            showSatellitesAtCloseApproach();
            earthViewController.triggerCollision(true);
            earthViewController.pauseSimulation();
            earthViewController.resumeSimulation();
        });
    }

    public void displaySatellites() {
        List<DisplaySatelliteModel> satellites = satelliteController.getTwoSatellitesSelected();

        if (satellites == null || satellites.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No satellite data available.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        earthViewController.resetState();
        Map<String, Ephemeris> ephemerisMap = new HashMap<>();
        Map<String, List<AbsoluteDate[]>> intervalMap = new HashMap<>();
        AbsoluteDate startDate = null;
        AbsoluteDate endDate = null;
        AbsoluteDate closeApproach = null;
        for (DisplaySatelliteModel model : satellites) {
            ephemerisMap.put(model.getName(), model.getEphemeris());
            intervalMap.put(model.getName(), Collections.singletonList(new AbsoluteDate[]{model.getStartDate(), model.getEndDate()}));
            if (startDate == null || model.getStartDate().compareTo(startDate) < 0) {
                startDate = model.getStartDate();
            }
            if (endDate == null || model.getEndDate().compareTo(endDate) > 0) {
                endDate = model.getEndDate();
            }
            closeApproach = model.getCloseApproachDate();
        }

        earthViewController.init(ephemerisMap, EarthViewController.wwd, earthViewController.getEarth(), startDate, endDate, intervalMap, closeApproach);
        earthViewController.startSimulation();
    }

    public void showSatellitesAtCloseApproach() {
        LoggerCustom.getInstance().logMessage("INFO: The satellites are on the close approach point");
        earthViewController.pauseSimulation();
        AbsoluteDate closeApproach = earthViewController.getCloseApproachDate();
        earthViewController.setStartDate(closeApproach);
        earthViewController.updateSatellites(closeApproach);
        EarthViewController.wwd.redraw();
        pauseButton.setDisable(false);
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

    public void loadSatelliteInformation() {
        try {
            FXMLLoader fxmlLoaderInformation = new FXMLLoader(getClass().getResource("/views/SatelliteInformation.fxml"));
            StackPane paneWithEarth = fxmlLoaderInformation.load();
            satelliteInformationController = fxmlLoaderInformation.getController();
            informationPane.getChildren().add(paneWithEarth);
            informationPane.setVisible(false);
        } catch (Exception ex) {
            System.out.println("An exception occurred due to can not instantiate the satellite information pane.");
        }
    }

    public void loadTableSatellite() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/TableSatelliteExtended.fxml"));
            BorderPane tableViewLayout = fxmlLoader.load();
            satelliteController = fxmlLoader.getController();
            successExtractList(tableViewLayout);
        } catch (IOException e) {
            e.printStackTrace();
        }
        extractDataTask();
    }

    private void extractDataTask() {
        extractDataButton.setOnMouseClicked(event -> {
            clearAllStates();
            String operator = setOperator(operatorBox.getValue());
            satelliteController.setThreshold(Integer.parseInt(thresholdBox.getText()));
            satelliteController.getSatelliteTable().getItems().clear();
            satelliteController.setTwoSatellitesSelected(new ArrayList<>());
            satelliteController.setDisplaySatelliteModels(new HashSet<>());

            Task<Void> task = createExtractDataTask(event, operator);
            setTaskOnFailed(task);
            new Thread(task).start();
        });
    }

    private Task<Void> createExtractDataTask(MouseEvent event, String operator) {
        return new Task<>() {
            @Override
            protected Void call() {
                startDataExtractionProcess();
                CollectSatelliteData collectSatelliteData = new CollectSatelliteData();
                Map<String, Item> listOfUniqueSatelliteTemp = collectSatelliteData.extractSatelliteData(predicateBox.getValue(), operator, valueField.getText());
                satelliteController.setListOfUniqueSatellite(listOfUniqueSatelliteTemp);
                loadAllTle(collectSatelliteData, listOfUniqueSatelliteTemp);
                handleDataExtractionCompletion(listOfUniqueSatelliteTemp, event);
                return null;
            }
        };
    }

    private void startDataExtractionProcess() {
        LoggerCustom.getInstance().logMessage("INFO: The process for extract the data is running...");
        mainPanel.setDisable(true);
        displayProgressBar();
    }

    private void handleDataExtractionCompletion(Map<String, Item> listOfUniqueSatelliteTemp, MouseEvent event) {
        Platform.runLater(() -> {
            progressBar.setVisible(false);
            if (listOfUniqueSatelliteTemp.isEmpty()) {
                showAlertAndLog(event);
            } else {
                completeDataExtractionSuccessfully();
            }
        });
    }

    private void showAlertAndLog(MouseEvent event) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "No results were found for the current settings", ButtonType.OK);
        satelliteController.getSatelliteTable().refresh();
        alert.showAndWait();
        LoggerCustom.getInstance().logMessage("INFO: Change configuration to find data");
        mainPanel.setDisable(false);
        event.consume();
    }

    private void completeDataExtractionSuccessfully() {
        mainPanel.setDisable(false);
        satelliteController.getSatelliteTable().refresh();
        informationPane.setVisible(true);
        updateCollisionInformation();
        LoggerCustom.getInstance().logMessage("INFO: Satellite's data were downloaded.");
    }


    private void displayProgressBar() {
        progressBar.setVisible(true);
        progressBar.setStyle("-fx-accent: #ff0000;"); // Change the color to red for better visibility
        progressBar.setScaleX(1.5);
        progressBar.setOpacity(1.0);
        progressBar.setScaleY(1.5);
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

    private void loadAllTle(CollectSatelliteData downloadTLE, Map<String, Item> listOfUniqueSatellite) {
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
                showSatelliteButton.setDisable(false);
                satelliteInformationController.setTextArea(newValue, thresholdBox.getText());
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
}