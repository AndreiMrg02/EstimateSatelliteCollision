package com.ucv.controller;

import com.ucv.Main;
import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import com.ucv.implementation.ConnectionService;
import com.ucv.implementation.DisplaySatelliteManager;
import com.ucv.tle.TleService;
import com.ucv.util.*;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.orekit.time.AbsoluteDate;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static com.ucv.database.DBOperation.clearAllStates;
import static com.ucv.database.HibernateUtil.closeSession;

public class MainController implements Initializable {

    private static final Logger logger = Logger.getLogger(MainController.class);

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
    private TleService tleService;
    private InternetConnectionData connectionData;
    private ConnectionService connectionService;
    private double xOffset = 0;
    private double yOffset = 0;
    private Task<Void> currentTask;
    private FieldValidator fieldValidator;
    private CustomAlert customAlert;
    private DisplaySatelliteManager displaySatelliteManager;

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
        this.customAlert = new CustomAlert();
        buttonSettings();
        roundedPane();
        loadEarth();
        drawEarthAfterInit();
        scrollPaneLog.setFitToWidth(true);
        fieldValidator = new FieldValidator(spaceTrackTleRadio, localTleRadio, valueField, thresholdBox, satelliteController);
        scrollPaneLog.setContent(loggerBox);
        LoggerCustom.getInstance().setConsole(loggerBox, scrollPaneLog);
        loadSatelliteInformation();
        displaySatelliteManager = new DisplaySatelliteManager(satelliteController, earthViewController);
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
        showSatellitesAction();

        stopSimulationAction();

        pauseAction();

        closeApproachAction();

        simulateCollisionAction();
    }

    private void pauseAction() {
        pauseButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: Simulation paused");
            earthViewController.pauseSimulation();
            resumeButton.setDisable(false);
            pauseButton.setDisable(true);
        });
    }

    private void closeApproachAction() {
        closeApproachButton.setOnAction(event -> showSatellitesAtCloseApproach());
        resumeButton.setOnAction(event -> {
            earthViewController.resumeSimulation();
            resumeButton.setDisable(true);
            pauseButton.setDisable(false);
            showSatellitesButton.setDisable(true);
        });
    }

    private void simulateCollisionAction() {
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

    private void showSatellitesAction() {
        showSatellitesButton.setOnAction(event -> {
            LoggerCustom.getInstance().logMessage("INFO: Check the map to see the satellites");
            displaySatelliteManager.displaySatellites();
            simulateCollision.setDisable(false);
            pauseButton.setDisable(false);
            stopSimulationButton.setDisable(false);
            closeApproachButton.setDisable(false);
            showSatellitesButton.setDisable(true);
            satelliteController.getSatelliteTable().setDisable(true);
        });
    }

    private void stopSimulationAction() {
        stopSimulationButton.setOnAction(event -> {
            closeButton.setDisable(false);
            LoggerCustom.getInstance().logMessage("INFO: The simulation was stopped");
            earthViewController.delete();
            showSatellitesButton.setDisable(false);
            simulateCollision.setDisable(true);
            pauseButton.setDisable(true);
            closeApproachButton.setDisable(true);
            stopSimulationButton.setDisable(true);
            satelliteInformationController.clearSatellitesStates();
            resumeButton.setDisable(true);
            extractDataButton.setDisable(false);
            earthViewController.triggerCollision(false);
            satelliteController.getSatelliteTable().setDisable(false);
        });
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
            if (!fieldValidator.validateInputs()) {
                return;
            }
            connectionService = new ConnectionService(connectionData);
            tleService = new TleService(connectionService, satelliteController, localTleRadio);
            Map<String, String[]> tleData = new HashMap<>();
            if (localTleRadio.isSelected()) {
                tleData = tleService.getTleData(mainPanel);
                if (tleData == null || tleData.isEmpty()) {
                    return;
                }
            }
            String operator = setOperator(operatorBox.getValue());
            resetDataForNewExtraction();

            currentTask = createSatelliteDataTask(tleData, operator, event);
            setTaskOnFailed(currentTask);
            new Thread(currentTask).start();
        });
    }


    private Task<Void> createSatelliteDataTask(Map<String, String[]> tleData, String operator, MouseEvent event) {
        return new Task<>() {
            @Override
            protected Void call() {
                LoggerCustom.getInstance().logMessage("INFO: The process for extracting the data is running...");

                Platform.runLater(() -> displayProgressBar());
                CollectSatelliteData collectSatelliteData = new CollectSatelliteData(connectionData);
                Map<String, Item> satelliteData = collectSatelliteData.extractSatelliteData("MIN_RNG", operator, valueField.getText());

                if (satelliteData.get("InvalidCredentials") != null) {
                    Platform.runLater(() -> {
                        customAlert.alertInvalidCredentials();
                        event.consume();
                    });
                    return null;
                }
                satelliteController.setListOfUniqueSatellite(satelliteData);
                if (!tleService.downloadTLEs(satelliteData, tleData, collectSatelliteData, spaceTrackTleRadio, informationPane)) {
                    cancelTask();
                    return null;
                }
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    if (satelliteData.isEmpty()) {
                        customAlert.alertNoResults(satelliteController, mainPanel);
                        event.consume();
                    }
                    setTaskOnSuccess();
                });

                return null;
            }
        };
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
        mainPanel.setDisable(true);
        menuPanel.setDisable(false);
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