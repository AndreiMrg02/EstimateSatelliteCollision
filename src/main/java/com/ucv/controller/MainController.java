package com.ucv.controller;

import com.ucv.Main;
import com.ucv.Util.LoggerCustom;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static com.ucv.database.DBOperation.clearAllStates;
import static com.ucv.database.HibernateUtil.closeSession;

public class MainController implements Initializable, SatelliteUpdateCallback {

    @FXML
    private ScrollPane scrollPaneLog;
    @FXML
    private TextArea thresholdBox;
    @FXML
    private TextFlow loggerBox;
    @FXML
    private Label satelliteOneLabel;
    @FXML
    private TextArea satelliteOneAltitude;
    @FXML
    private TextArea longitudeSatelliteOne;
    @FXML
    private TextArea latitudeSatelliteOne;
    @FXML
    private Label satelliteTwoLabel;
    @FXML
    private TextArea satelliteTwoAltitude;
    @FXML
    private TextArea longitudeSatelliteTwo;
    @FXML
    private TextArea latitudeSatelliteTwo;
    @FXML
    private VBox configurationPane;
    @FXML
    private StackPane satelliteOnePane;
    @FXML
    private StackPane satelliteTwoPane;
    @FXML
    private TextArea speedSatelliteTwo;
    @FXML
    private TextArea speedSatelliteOne;
    @FXML
    private StackPane paneInformationCollision;
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
    private TextArea pcTextArea;
    @FXML
    private TextArea closeApproachDistanceTextArea;
    @FXML
    private TextArea closeApproachDateTextArea;
    @FXML
    private TextArea thresholdTextArea;
    @FXML
    private TextArea startDateTextArea;
    @FXML
    private TextArea endDateTextArea;
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
    private EarthController earthController;
    private SatelliteController satelliteController;

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

        File orekitData = new File("data/orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));


        ObservableList<String> predicateList = FXCollections.observableArrayList();
        ObservableList<String> operatorList = FXCollections.observableArrayList();
        addPredicateToList(predicateList);
        predicateBox.setItems(predicateList);

        addOperatorToList(operatorList);
        operatorBox.setItems(operatorList);
        progressBar.setProgress(-1.0);
        progressBar.setVisible(false);

        predicateBox.setValue("MIN_RNG");
        operatorBox.setValue("<");
        valueField.setText("10");
        setButtonStyle(showSatelliteButton);
        setButtonStyle(pauseButton);
        setButtonStyle(closeApproachButton);
        loadEarth();
        loadTableSatellite();
        closeSession();
        buttonFunction();
        roundedPane();
        scrollPaneLog.setFitToWidth(true);
        scrollPaneLog.setContent(loggerBox);
        LoggerCustom.getInstance().setConsole(loggerBox,scrollPaneLog);

        earthController.setCallback(this);

    }

    private void roundedPane() {
        addClip(paneInformationCollision, 540, 256, 30, 30);
        addClip(configurationPane, 30, 30);
        addClip(satelliteOnePane, 30, 30);
        addClip(satelliteTwoPane, 30, 30);
        addClip(earthPane,30,30);
        addClip(loggerBox,15,15);
        addClip(scrollPaneLog,16,16);
    }

    private void addClip(Region region, double arcWidth, double arcHeight) {
        region.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            Rectangle clip = new Rectangle(newValue.getWidth(), newValue.getHeight());
            clip.setArcWidth(arcWidth);
            clip.setArcHeight(arcHeight);
            region.setClip(clip);
        });
    }

    private void addClip(Region region, double width, double height, double arcWidth, double arcHeight) {
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(arcWidth);
        clip.setArcHeight(arcHeight);
        region.setClip(clip);
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
            earthController.delete();
            clearSatellitesDataFromFields();
            showSatelliteButton.setDisable(false);
            pauseButton.setDisable(true);
            closeApproachButton.setDisable(true);
            stopSimulationButton.setDisable(true);
            earthController.triggerCollision(false);
            satelliteController.getSatelliteTable().setDisable(false);

        });
        pauseButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: Simulation paused");
            earthController.pauseSimulation();
            resumeButton.setDisable(false);
            pauseButton.setDisable(true);
        });

        closeApproachButton.setOnAction(event -> showSatellitesAtCloseApproach());
        resumeButton.setOnAction(event -> {
            earthController.resumeSimulation();
            pauseButton.setDisable(false);
            showSatelliteButton.setDisable(true);
        });
        simulateCollision.setOnAction(event -> {
            showSatellitesAtCloseApproach();
            earthController.triggerCollision(true);
            earthController.pauseSimulation();
            earthController.resumeSimulation();
        });
    }

    public void displaySatellites() {
        List<DisplaySatelliteModel> satellites = satelliteController.getTwoSatellitesSelected();

        if (satellites == null || satellites.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No satellite data available.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        earthController.resetState();

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

        earthController.init(ephemerisMap, EarthController.wwd, earthController.getEarth(), startDate, endDate, intervalMap, closeApproach);
        earthController.startSimulation();
    }

    public void showSatellitesAtCloseApproach() {
        LoggerCustom.getInstance().logMessage("INFO: The satellites are on the close approach point");
        earthController.pauseSimulation();
        AbsoluteDate closeApproach = earthController.getCloseApproachDate();
        earthController.setStartDate(closeApproach);
        earthController.updateSatellites(closeApproach);
        EarthController.wwd.redraw();
        pauseButton.setDisable(false);
    }


    public void loadEarth() {
        try {
            FXMLLoader fxmlLoaderEarth = new FXMLLoader(getClass().getResource("/views/EarthViewNou.fxml"));
            StackPane paneWithEarth = fxmlLoaderEarth.load();
            earthController = fxmlLoaderEarth.getController();
            earthPane.getChildren().add(paneWithEarth);
        } catch (Exception ex) {
            System.out.println("An exception occurred due to can not instantiate the earth pane.");
            ex.printStackTrace();
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
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    LoggerCustom.getInstance().logMessage("INFO: The process for extract the data is running...");
                    mainPanel.setDisable(true);
                    displayProgressBar();
                    CollectSatelliteData collectSatelliteData = new CollectSatelliteData();
                    Map<String, Item> listOfUniqueSatelliteTemp = collectSatelliteData.extractSatelliteData(predicateBox.getValue(), operator, valueField.getText());
                    satelliteController.setListOfUniqueSatellite(listOfUniqueSatelliteTemp);
                    loadAllTle(collectSatelliteData, listOfUniqueSatelliteTemp);
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        if (listOfUniqueSatelliteTemp.isEmpty()) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "No results were found for the current settings", ButtonType.OK);
                            satelliteController.getSatelliteTable().refresh();
                            alert.showAndWait();
                            LoggerCustom.getInstance().logMessage("INFO: Change configuration to find data");
                            mainPanel.setDisable(false);
                            event.consume();
                        } else {
                            mainPanel.setDisable(false);
                            satelliteController.getSatelliteTable().refresh();
                            setTextArea();
                            LoggerCustom.getInstance().logMessage("INFO: Satellite's data were downloaded.");
                        }
                    });
                    return null;
                }
            };
            setTaskOnFailed(task);
            new Thread(task).start();
        });
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
                newTca = item.getTca().replaceAll(":", colonForSpaceTrack);
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

    private void setButtonStyle(Button button) {
        button.setOnMouseEntered(e -> {
            button.getStyleClass().add("buttonBackgroundHovered");
            ScaleTransition st = new ScaleTransition(Duration.millis(500), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), button);
            tt.setToY(-10);
            tt.play();
        });

        button.setOnMouseExited(e -> {
            button.getStyleClass().remove("buttonBackgroundHovered");
            ScaleTransition st = new ScaleTransition(Duration.millis(500), button);
            st.setToX(1);
            st.setToY(1);
            st.play();
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), button);
            tt.setToY(0);
            tt.play();
        });
    }

    private void setTextArea() {
        satelliteController.getSatelliteTable().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showSatelliteButton.setDisable(false);
                pcTextArea.setText(newValue.getCollisionProbability());
                startDateTextArea.setText(newValue.getStartDate());
                endDateTextArea.setText(newValue.getEndDate());
                closeApproachDateTextArea.setText(newValue.getCloseApproachDate());
                closeApproachDistanceTextArea.setText(newValue.getCloseApproach());
                satelliteOneLabel.setText(newValue.getSat1Name());
                satelliteTwoLabel.setText(newValue.getSat2Name());
                thresholdTextArea.setText(thresholdBox.getText());
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
}