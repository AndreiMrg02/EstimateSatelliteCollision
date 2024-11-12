package com.ucv.controller;

import com.ucv.datamodel.satellite.CollisionData;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.SpatialObject;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollisionTask;
import com.ucv.implementation.TlePropagator;
import com.ucv.util.LoggerCustom;
import com.ucv.util.SatelliteFileHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.orekit.propagation.SpacecraftState;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.ucv.database.DBOperation.getSatellitesName;
import static com.ucv.database.DBOperation.getStatesBySatelliteName;


public class SatelliteController implements Initializable {
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private final Logger logger = LogManager.getLogger(SatelliteController.class);
    @FXML
    private TableView<CollisionData> satelliteTable;
    @FXML
    private TableColumn<CollisionData, String> satOneNameColumn = new TableColumn<>("Satellite One Name");
    @FXML
    private TableColumn<CollisionData, String> satTwoNameColumn = new TableColumn<>("Satellite Two Name");
    private Map<String, SpatialObject> spatialObjectList;
    private Set<DisplaySatelliteModel> displaySatelliteModels;
    private List<DisplaySatelliteModel> selectedSatellites;
    private List<CollisionData> collisionDataList;
    private List<TlePropagator> tleThreads;
    private int threshold;
    private SatelliteFileHandler satelliteFileHandler;

    public void setDisplaySatelliteModels(Set<DisplaySatelliteModel> displaySatelliteModels) {
        this.displaySatelliteModels = displaySatelliteModels;
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        spatialObjectList = new HashMap<>();
        ArrayList<String> listOfTle = new ArrayList<>();
        displaySatelliteModels = new LinkedHashSet<>();
        collisionDataList = new ArrayList<>();
        satOneNameColumn.setCellValueFactory(new PropertyValueFactory<>("sat1Name"));
        satTwoNameColumn.setCellValueFactory(new PropertyValueFactory<>("sat2Name"));
        selectedSatellites = new ArrayList<>();
        tleThreads = new ArrayList<>();
        satelliteTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String satellite1Name = newValue.getSat1Name();
                String satellite2Name = newValue.getSat2Name();

                selectedSatellites = new ArrayList<>();
                for (DisplaySatelliteModel model : displaySatelliteModels) {
                    if (model.getName().equals(satellite1Name) || model.getName().equals(satellite2Name)) {
                        selectedSatellites.add(model);
                    }
                }
            }
        });
        satelliteFileHandler = new SatelliteFileHandler(satelliteTable, listOfTle, spatialObjectList);
    }

    public void manageSatellites() {
        satelliteFileHandler.addTLEsToTextFile();
        LoggerCustom.getInstance().logMessage("The process to save states in data has started...");
        for (SpatialObject spatialObject : spatialObjectList.values()) {
            TlePropagator object = new TlePropagator(spatialObject);
            object.start();
            tleThreads.add(object);
        }
        for (TlePropagator threadTLE : tleThreads) {
            try {
                threadTLE.join();
            } catch (InterruptedException e) {
                logger.error(String.format("Thread interrupted: %s", e.getMessage()));
                // Re-interrupt the current thread to maintain the interrupted status
                Thread.currentThread().interrupt();
            }
        }
        LoggerCustom.getInstance().logMessage("The process to save states in data base stopped.");
        estimateCollisionBetweenSatellites();
    }


    private void estimateCollisionBetweenSatellites() {
        LoggerCustom.getInstance().logMessage("The collision risk estimation process has started...");

        List<String> satelliteNames = getSatellitesName();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            processCollision(satelliteNames, executor);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        Platform.runLater(() -> {
            satelliteTable.setItems(FXCollections.observableList(collisionDataList));
            satelliteTable.refresh();
        });
    }

    private void processCollision(List<String> satelliteNames, ExecutorService executor) {
        int size = satelliteNames.size();
        for (int i = 0; i < size; i++) {
            final String satelliteOneName = satelliteNames.get(i);
            final List<SpacecraftState> statesOne = getStatesBySatelliteName(satelliteOneName);
            for (int j = i + 1; j < size; j++) {
                final String satelliteTwoName = satelliteNames.get(j);
                final List<SpacecraftState> statesTwo = getStatesBySatelliteName(satelliteTwoName);
                CollisionTask task = new CollisionTask(statesOne, statesTwo, satelliteOneName, satelliteTwoName, displaySatelliteModels, collisionDataList, threshold);
                executor.submit(task);
            }
        }
    }

    /*
     * To avoid duplicate data the satellites will be filtered by TLE. I created a map that had the TCA key
     */
    public void setListOfUniqueSatellite(Map<String, Item> listOfUniqueSatellite) {
        items.addAll(new ArrayList<>(listOfUniqueSatellite.values()));
        satelliteTable.getItems().addAll(collisionDataList);
    }

    /*
     * Set spatial object for create Propagator.
     */
    public void addSpatialObject(String tca, String satName, String tle) {
        SpatialObject spatialObject = new SpatialObject();
        spatialObject.setName(satName);
        spatialObject.setTle(tle);
        spatialObject.setTca(tca);
        spatialObjectList.put(satName, spatialObject);
    }

    public TableView<CollisionData> getSatelliteTable() {
        return satelliteTable;
    }

    public List<DisplaySatelliteModel> getTwoSatellitesSelected() {
        return selectedSatellites;
    }

    public void setTwoSatellitesSelected(List<DisplaySatelliteModel> twoSatellitesSelected) {
        this.selectedSatellites = twoSatellitesSelected;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void extractTleToFile() {
        satelliteFileHandler.extractTleToFile();
    }
}
