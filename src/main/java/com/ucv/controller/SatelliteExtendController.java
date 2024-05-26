package com.ucv.controller;

import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.SpatialObject;
import com.ucv.datamodel.satellite.SpatialObjectTableModel;
import com.ucv.datamodel.xml.Item;
import com.ucv.run.CollectSatelliteData;
import com.ucv.run.CollisionTask;
import com.ucv.satellite.TlePropagator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.orekit.propagation.SpacecraftState;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.ucv.database.DBManager.getStatesAllSatelliteName;
import static com.ucv.database.DBManager.getStatesBySatelliteName;


public class SatelliteExtendController implements Initializable {
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private Map<String, Item> listOfUniqueSatellite = new HashMap<>();
    private ArrayList<String> listOfTle;
    private Map<String, SpatialObject> spatialObjectList;
    private Set<DisplaySatelliteModel> stringDisplaySatelliteModelMap;
    @FXML
    private Button viewTLEButton;
    private List<DisplaySatelliteModel> twoSatellitesSelected;
    @FXML
    private TableView<SpatialObjectTableModel> satelliteTable; // Înlocuiți "SatelliteModel" cu tipul de date real
    @FXML
    private TableColumn<SpatialObjectTableModel, String> startDateColumn = new TableColumn<>("START_DATE");
    @FXML
    private TableColumn<SpatialObjectTableModel, String> endDateColumn = new TableColumn<>("END_DATE");
    @FXML
    private TableColumn<SpatialObjectTableModel, String> closeApproachColumn = new TableColumn<>("CLOSE_APPROACH");
    @FXML
    private TableColumn<SpatialObjectTableModel, String> closeApproachDateColumn = new TableColumn<>("CLOSE_APPROACH_DATE");
    @FXML
    private TableColumn<SpatialObjectTableModel, String> sat1NameColumn = new TableColumn<>("SAT_1_NAME");
    @FXML
    private TableColumn<SpatialObjectTableModel, String> sat2NameColumn = new TableColumn<>("SAT_2_NAME");
    @FXML
    private TableColumn<SpatialObjectTableModel, String> pcColumn = new TableColumn<>("COLLISION_PROBABILITY");

    private List<SpatialObjectTableModel> spatialObjectTableModels;

    public Set<DisplaySatelliteModel> getStringDisplaySatelliteModelMap() {
        return stringDisplaySatelliteModelMap;
    }

    public void setStringDisplaySatelliteModelMap(Set<DisplaySatelliteModel> stringDisplaySatelliteModelMap) {
        this.stringDisplaySatelliteModelMap = stringDisplaySatelliteModelMap;
    }

    /*

     * Write Tle in a file

     */
    public static void writeToFile(List<String> content, String directoryPath, String fileName) {
        Path filePath = Paths.get(directoryPath, fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toString()))) {
            for (String line : content) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Datele au fost scrise cu succes în " + filePath);
        } catch (IOException e) {
            System.err.println("Eroare la scrierea în fisier: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize your lists and sets
        spatialObjectList = new HashMap<>();
        listOfTle = new ArrayList<>();
        stringDisplaySatelliteModelMap = new LinkedHashSet<>();
        spatialObjectTableModels = new ArrayList<>();
        // Setting up the TableColumn cell value factories
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        closeApproachColumn.setCellValueFactory(new PropertyValueFactory<>("closeApproach"));
        closeApproachDateColumn.setCellValueFactory(new PropertyValueFactory<>("closeApproachDate"));
        sat1NameColumn.setCellValueFactory(new PropertyValueFactory<>("sat1Name"));
        sat2NameColumn.setCellValueFactory(new PropertyValueFactory<>("sat2Name"));
        pcColumn.setCellValueFactory(new PropertyValueFactory<>("collisionProbability"));
        twoSatellitesSelected = new ArrayList<>();
        getSelectedSatellites();
    }

    private void getSelectedSatellites() {

        satelliteTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String satellite1Name = newValue.getSat1Name();
                String satellite2Name = newValue.getSat2Name();

                twoSatellitesSelected = new ArrayList<>();
                for (DisplaySatelliteModel model : stringDisplaySatelliteModelMap) {
                    if (model.getName().equals(satellite1Name) || model.getName().equals(satellite2Name)) {
                        twoSatellitesSelected.add(model);
                    }
                }
                System.out.println("Selected sattelite One: " + satellite1Name);
                System.out.println("Selected sattelite two: " + satellite2Name);

            }
        });
    }

    /*

     * This function load all tle's from the space track website.
     *  For each satellite will be executed a query.

     */

    private void loadAllTle() {
        String specificTagBuilder = "%3C";
        String epochDesc = "%20";
        String queryFirstSatellite = "";
        String querySecondSatellite = "";
        String colonForSpaceTrack = "%3A";
        CollectSatelliteData downloadTLE = new CollectSatelliteData();
        String newTca = "";
        Map<String, String> listOfTlePerSattelite = new HashMap<>();
        for (Item item : listOfUniqueSatellite.values()) {
            if (item.getTca().contains(":")) {
                newTca = item.getTca().replaceAll(":", colonForSpaceTrack);
            }

            queryFirstSatellite = String.format("/basicspacedata/query/class/tle/NORAD_CAT_ID/%s/EPOCH/%s%s/orderby/EPOCH%sdesc/limit/1/format/tle/emptyresult/show", item.getSat1Id(), specificTagBuilder, newTca, epochDesc);
            String tleSatellite = downloadTLE.loadTLEs(queryFirstSatellite);
            if (!tleSatellite.isEmpty()) {
                listOfTle.add(tleSatellite);
                addSpatialObject(item.getTca(), item.getSat1Name(), tleSatellite);
                listOfTlePerSattelite.put(tleSatellite, item.getSat1Name());
            }
            querySecondSatellite = String.format("/basicspacedata/query/class/tle/NORAD_CAT_ID/%s/EPOCH/%s%s/orderby/EPOCH%sdesc/limit/1/format/tle/emptyresult/show", item.getSat2Id(), specificTagBuilder, newTca, epochDesc);
            tleSatellite = downloadTLE.loadTLEs(querySecondSatellite);
            if (!tleSatellite.isEmpty()) {
                listOfTle.add(tleSatellite);
                addSpatialObject(item.getTca(), item.getSat2Name(), tleSatellite);
                listOfTlePerSattelite.put(tleSatellite, item.getSat2Name());
            }
        }
        addAllSpatialObjectInDB();

    }

    private void addAllSpatialObjectInDB() {
        List<TlePropagator> threads = new ArrayList<>();
        for (SpatialObject spatialObject : spatialObjectList.values()) {
            TlePropagator object = new TlePropagator(spatialObject);
            object.start();
            threads.add(object);
            System.out.println("Au fost adaugate starile pentru satelitul: " + spatialObject.getName() + "cu TLE-ul: " + spatialObject.getTle());
        }

        // asteapta ca firele de executie create anterior sa isi termine executia
        for (TlePropagator p : threads) {
            try {
                p.join();
            } catch (InterruptedException e) {
                System.out.println("Eroare: " + e.getMessage());
            }
        }
        estimateCollisionBetweenAllSatellites();

    }

    private void estimateCollisionBetweenAllSatellites() {
        List<String> satelliteNames = getStatesAllSatelliteName();
        int size = satelliteNames.size();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            for (int i = 0; i < size; i++) {
                final String entryOne = satelliteNames.get(i);
                final List<SpacecraftState> statesOne = getStatesBySatelliteName(entryOne);
                for (int j = i + 1; j < size; j++) {
                    final String entryTwo = satelliteNames.get(j);
                    final List<SpacecraftState> statesTwo = getStatesBySatelliteName(entryTwo);

                    CollisionTask task = new CollisionTask(statesOne, statesTwo, entryOne, entryTwo, stringDisplaySatelliteModelMap, spatialObjectTableModels);
                    executor.submit(task);
                }
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        Platform.runLater(() -> {
            satelliteTable.setItems(FXCollections.observableList(spatialObjectTableModels));
            satelliteTable.refresh();
        });
    }

    /*
     * To avoid duplicate data the satellites will be filtered by TLE. I created a map that had the TCA key
     */
    public void setListOfUniqueSatellite(Map<String, Item> listOfUniqueSatellite) {
        this.listOfUniqueSatellite = listOfUniqueSatellite;
        items.addAll(new ArrayList<>(listOfUniqueSatellite.values()));
        satelliteTable.getItems().addAll(spatialObjectTableModels);
        loadAllTle();
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
    public void extractTleToListView() {
        viewTLEButton.setOnMouseClicked(event -> {
            ListView<String> listView = new ListView<>();
            ObservableList<String> items = FXCollections.observableArrayList();
            items.addAll(listOfTle);
            listView.setItems(items);

            VBox vbox = new VBox(listView);
            Scene scene = new Scene(vbox, 450, 200);
            Stage stage = new Stage();

            stage.setTitle("All Tle");
            stage.setScene(scene);
            stage.show();
        });
    }

    public void extractTleToFile() {
        writeToFile(listOfTle, "E:\\Licenta\\EstimareRiscColiziuneSateliti\\src\\main\\resources\\TleFolder", "ExtractedTLE.txt");
    }


    public TableView<SpatialObjectTableModel> getSatelliteTable() {
        return satelliteTable;
    }

    public void setSatelliteTable(TableView<SpatialObjectTableModel> satelliteTable) {
        this.satelliteTable = satelliteTable;
    }

    public List<DisplaySatelliteModel> getTwoSatellitesSelected() {
        return twoSatellitesSelected;
    }

    public void setTwoSatellitesSelected(List<DisplaySatelliteModel> twoSatellitesSelected) {
        this.twoSatellitesSelected = twoSatellitesSelected;
    }
}
