package com.ucv.controller;

import com.ucv.datamodel.satellite.CollisionData;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.SpatialObject;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollisionTask;
import com.ucv.implementation.TlePropagator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ucv.database.DBOperation.getSatellitesName;
import static com.ucv.database.DBOperation.getStatesBySatelliteName;


public class SatelliteController implements Initializable {
    @FXML
    private TableView<CollisionData> satelliteTable;
    @FXML
    private TableColumn<CollisionData, String> satOneNameColumn = new TableColumn<>("Satellite One Name");
    @FXML
    private TableColumn<CollisionData, String> satTwoNameColumn = new TableColumn<>("Satellite Two Name");
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private ArrayList<String> listOfTle;
    private Map<String, SpatialObject> spatialObjectList;
    private Set<DisplaySatelliteModel> displaySatelliteModels;
    private List<DisplaySatelliteModel> selectedSatellites;
    private List<CollisionData> collisionDataList;
    private List<TlePropagator> tleThreads;
    private int threshold;

    public void setDisplaySatelliteModels(Set<DisplaySatelliteModel> displaySatelliteModels) {
        this.displaySatelliteModels = displaySatelliteModels;
    }

    /*
     * Write Tle in a file
     */
    private void writeToFile(String directoryPath, String fileName) {
        File file = new File(directoryPath, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            for (String tle : listOfTle) {
                writer.write(tle + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        spatialObjectList = new HashMap<>();
        listOfTle = new ArrayList<>();
        displaySatelliteModels = new LinkedHashSet<>();
        collisionDataList = new ArrayList<>();
        satOneNameColumn.setCellValueFactory(new PropertyValueFactory<>("sat1Name"));
        satTwoNameColumn.setCellValueFactory(new PropertyValueFactory<>("sat2Name"));
        selectedSatellites = new ArrayList<>();
        tleThreads = new ArrayList<>();
        getSelectedSatellites();
    }

    private void getSelectedSatellites() {
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
    }
    /*
     * This function load all tle's from the space track website.
     *  For each satellite will be executed a query.
     */

    public void manageSatellites() {
        addTLEsToTextFile();
        for (SpatialObject spatialObject : spatialObjectList.values()) {
            TlePropagator object = new TlePropagator(spatialObject);
            object.start();
            tleThreads.add(object);
            System.out.println("Au fost adaugate starile pentru satelitul: " + spatialObject.getName() + "cu TLE-ul: " + spatialObject.getTle());
        }
        for (TlePropagator threadTLE : tleThreads) {
            try {
                threadTLE.join();
            } catch (InterruptedException e) {
                System.out.println("Eroare: " + e.getMessage());
            }
        }
        estimateCollisionBetweenSatellites();
    }

    private void addTLEsToTextFile() {
        listOfTle.addAll(spatialObjectList.values().stream().flatMap(spatialObject -> {
            TLE tle = spatialObject.getTle();
            if (tle != null) {
                return Stream.of(tle.getLine1(), tle.getLine2(), "");
            } else {
                return Stream.of();
            }
        }).collect(Collectors.toList()));
    }

    private void estimateCollisionBetweenSatellites() {
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
        spatialObject.setPropertiesFromString(tle, satName);
        spatialObject.setTca(tca);
        spatialObjectList.put(satName, spatialObject);
    }

    /*
     * That function is used to add the tle list in the list view.
     */

    public void extractTleToFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the location to save TLEs");
        fileChooser.setInitialFileName("ExtractedTLE.txt");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(satelliteTable.getScene().getWindow());

        if (file != null) {
            writeToFile(file.getParent(), file.getName());
        }
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
}
