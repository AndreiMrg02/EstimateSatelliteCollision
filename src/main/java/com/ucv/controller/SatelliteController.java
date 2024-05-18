package com.ucv.controller;


import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.PositionDifference;
import com.ucv.datamodel.satellite.SpatialObject;
import com.ucv.datamodel.xml.Item;
import com.ucv.run.DownloadTLE;
import com.ucv.satellite.TlePropagator;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.ucv.database.DBManager.getStatesAllSatelliteName;
import static com.ucv.database.DBManager.getStatesBySatelliteName;


public class SatelliteController implements Initializable {
    private ObservableList<Item> items = FXCollections.observableArrayList();
    private Map<String, Item> listOfUniqueSatellite = new HashMap<>();
    private ArrayList<String> listOfTle;
    private List<SpatialObject> spatialObjectList;
    private Set<DisplaySatelliteModel> stringDisplaySatelliteModelMap;
    @FXML
    private Button viewTLEButton;
    @FXML
    private Button extractTleToFileButton;
    @FXML
    private BorderPane satellitePane;
    @FXML
    private TableView<Item> satelliteTable; // Înlocuiți "SatelliteModel" cu tipul de date real
    @FXML
    private TableColumn<Item, String> cdmIdColumn = new TableColumn<>("CDM_ID");
    @FXML
    private TableColumn<Item, String> tcaColumn = new TableColumn<>("TCA");
    @FXML
    private TableColumn<Item, String> sat1NameColumn = new TableColumn<>("SAT_1_NAME");
    @FXML
    private TableColumn<Item, String> sat2NameColumn = new TableColumn<>("SAT_2_NAME");
    @FXML
    private TableColumn<Item, String> pcColumn = new TableColumn<>("PC");
    private List<String> satName = new ArrayList<>();
    public Set<DisplaySatelliteModel> getStringDisplaySatelliteModelMap() {
        return stringDisplaySatelliteModelMap;
    }

    public void setStringDisplaySatelliteModelMap(Set<DisplaySatelliteModel> stringDisplaySatelliteModelMap) {
        this.stringDisplaySatelliteModelMap = stringDisplaySatelliteModelMap;
    }

    /*

     * Write Tle in a file

     */
    public static void writeToFile(ArrayList<String> content, String directoryPath, String fileName) {
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
        spatialObjectList = new ArrayList<>();
        cdmIdColumn.setCellValueFactory(new PropertyValueFactory<>("cdmId"));
        tcaColumn.setCellValueFactory(new PropertyValueFactory<>("tca"));
        sat1NameColumn.setCellValueFactory(new PropertyValueFactory<>("sat1Name"));
        sat2NameColumn.setCellValueFactory(new PropertyValueFactory<>("sat2Name"));
        pcColumn.setCellValueFactory(new PropertyValueFactory<>("pc"));
        listOfTle = new ArrayList<>();
        stringDisplaySatelliteModelMap = new LinkedHashSet<>();

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
        DownloadTLE downloadTLE = new DownloadTLE();
        String newTca = "";

        for (Item item : listOfUniqueSatellite.values()) {
            if (item.getTca().contains(":")) {
                newTca = item.getTca().replaceAll(":", colonForSpaceTrack);
            }

            queryFirstSatellite = String.format("/basicspacedata/query/class/tle/NORAD_CAT_ID/%s/EPOCH/%s%s/orderby/EPOCH%sdesc/limit/1/format/tle/emptyresult/show", item.getSat1Id(), specificTagBuilder, newTca, epochDesc);
            String tleSatellite = downloadTLE.loadTleSatellite(queryFirstSatellite);
            if (!tleSatellite.isEmpty()) {
                listOfTle.add(tleSatellite);
                addSpatialObject(item.getTca(), item.getSat1Name(), tleSatellite);
            }
            querySecondSatellite = String.format("/basicspacedata/query/class/tle/NORAD_CAT_ID/%s/EPOCH/%s%s/orderby/EPOCH%sdesc/limit/1/format/tle/emptyresult/show", item.getSat2Id(), specificTagBuilder, newTca, epochDesc);
            tleSatellite = downloadTLE.loadTleSatellite(querySecondSatellite);
            if (!tleSatellite.isEmpty()) {
                listOfTle.add(tleSatellite);
                addSpatialObject(item.getTca(), item.getSat2Name(), tleSatellite);
            }
        }
        addAllSpatialObjectInDB();

    }

    private void addAllSpatialObjectInDB() {
        List<TlePropagator> threads = new ArrayList<>();
        for (SpatialObject spatialObject : spatialObjectList) {
            TlePropagator object = new TlePropagator(spatialObject);
            object.start();
            threads.add(object);
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

        for (int i = 0; i < size; i++) {
            String entryOne = satelliteNames.get(i);
            List<SpacecraftState> statesOne = getStatesBySatelliteName(entryOne);
            for (int j = i + 1; j < size; j++) {
                String entryTwo = satelliteNames.get(j);
                List<SpacecraftState> statesTwo = getStatesBySatelliteName(entryTwo);

                List<SpacecraftState> stateOneList = new ArrayList<>(statesOne);
                List<SpacecraftState> stateTwoList = new ArrayList<>(statesTwo);

                estimateCollision(stateOneList, stateTwoList, entryOne, entryTwo);
            }
        }
    }



    private void estimateCollision(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList, String entryOne, String entryTwo) {
        Ephemeris ephemerisSatelliteOne = new Ephemeris(stateOneList, 4);
        Ephemeris ephemerisSatelliteTwo = new Ephemeris(stateTwoList, 4);
        final PositionDifference closestApproach = new PositionDifference();
        AbsoluteDate startDate = extractStartDate(stateOneList, stateTwoList);
        if (startDate == null) {
            System.out.println("There is no common start date between:" + entryOne +
                    " and " + entryTwo);
            return;
        }
        AbsoluteDate endDate = extractEndDate(stateOneList, stateTwoList);
        if (endDate == null) {
            System.out.println("There is no common end date between:" + entryOne +
                    " and " + entryTwo);
            return;
        }

        if (!satName.contains(entryOne)) {
            stringDisplaySatelliteModelMap.add(new DisplaySatelliteModel(startDate, endDate, entryOne, ephemerisSatelliteOne, stateOneList));
        }
        satName.add(entryOne);

        // Propagate both ephemeris to find the closest approach
        ephemerisSatelliteOne.setStepHandler(60, currentState -> {
            SpacecraftState stateTwo = ephemerisSatelliteTwo.propagate(currentState.getDate());
            Vector3D positionDifference = currentState.getPosition().subtract(stateTwo.getPosition());
            double distance = positionDifference.getNorm();
            if (closestApproach.getDifference() > distance) {
                closestApproach.setDifference(distance);
                closestApproach.setDate(currentState.getDate());
            }
        });

        // Propagate both ephemeris
        ephemerisSatelliteOne.propagate(startDate, endDate);
        // Clear step handlers after propagation
        ephemerisSatelliteOne.clearStepHandlers();
        // Once the closest approach is found, estimate collision probability based on closest approach distance
        // Calculate collision probability
        final double threshold = 3500; // Set your threshold value here
        double collisionProbability = estimateCollisionProbability(closestApproach.getDifference(), threshold);
        System.out.println("Closest approach between Satellites: " + entryOne + " and " + entryTwo + " is: " + closestApproach.getDifference() + " meters at date: " + closestApproach.getDate().toDate(TimeScalesFactory.getUTC()));
        System.out.println("Collision probability is: " + String.format("%.10f%%", collisionProbability));
    }

    private double estimateCollisionProbability(double closestApproachDistance, double threshold) {
        double scale = 0.3;  // Factorul de scalare pentru a ajusta sensibilitatea
        double ratio = (closestApproachDistance / threshold);
        double collisionProbability = Math.exp(-scale * ratio);  // Aplică factorul de scalare în exponent
        return collisionProbability * 100;  // Converteste în procente
    }


    private AbsoluteDate extractStartDate(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {
        if (!stateOneList.isEmpty() && !stateTwoList.isEmpty()) {
            for (SpacecraftState stateOne : stateOneList) {
                for (SpacecraftState stateTwo : stateTwoList) {
                    if (stateOne.getDate().equals(stateTwo.getDate())) {
                        return stateOne.getDate();
                    }
                }
            }
        }
        return null;
    }

    private AbsoluteDate extractEndDate(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {
        if (!stateOneList.isEmpty() && !stateTwoList.isEmpty()) {
            for (int i = stateOneList.size() - 1; i >= 0; i--) {
                for (int j = stateTwoList.size() - 1; j >= 0; j--) {
                    if (stateOneList.get(i).getDate().equals(stateTwoList.get(j).getDate())) {
                        return stateOneList.get(i).getDate();
                    }
                }
            }
        }
        return null;
    }

    /*

     * To avoid duplicate data the satellites will be filtered by TLE. I created a map that had the TCA key

     */
    public void setListOfUniqueSatellite(Map<String, Item> listOfUniqueSatellite) {
        this.listOfUniqueSatellite = listOfUniqueSatellite;
        items.addAll(new ArrayList<>(listOfUniqueSatellite.values()));
        satelliteTable.setItems(items);
        loadAllTle();
    }

    /*

     * Set spatial object for create Propagator.

     */
    public void addSpatialObject(String tca, String satName, String tle) {

        SpatialObject spatialObject = new SpatialObject();
        spatialObject.setPropertiesFromString(tle, satName);
        spatialObject.setTca(tca);
        spatialObjectList.add(spatialObject);
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

    public TableView<Item> getSatelliteTable() {
        return satelliteTable;
    }


}
