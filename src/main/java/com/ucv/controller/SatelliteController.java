package com.ucv.controller;


import com.ucv.run.DownloadTLE;
import com.ucv.satellite.PositionDifference;
import com.ucv.satellite.SpatialObject;
import com.ucv.satellite.TlePropagator;
import com.ucv.xml.model.Item;
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

import static com.ucv.database.DBManager.getStatesBySatelliteName;


public class SatelliteController implements Initializable {
    ObservableList<Item> items = FXCollections.observableArrayList();
    Map<String, Item> listOfUniqueSatellite = new HashMap<>();
    ArrayList<String> listOfTle;
    List<SpatialObject> spatialObjectList;
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
        LinkedHashSet<SpacecraftState> statesOne = getStatesBySatelliteName("FENGYUN 1C DEB");
        LinkedHashSet<SpacecraftState> statesTwo = getStatesBySatelliteName("SL-8 R/B");
        List<SpacecraftState> stateOneList = new ArrayList<>(statesOne);
        List<SpacecraftState> stateTwoList = new ArrayList<>(statesTwo);

        estimateCollision(stateOneList, stateTwoList);
    }


    private void estimateCollision(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {

        Ephemeris ephemerisSatelliteOne = new Ephemeris(stateOneList, 4);
        Ephemeris ephemerisSatelliteTwo = new Ephemeris(stateTwoList, 4);
        final PositionDifference difference = new PositionDifference();

        AbsoluteDate startDate = extractStartDate(stateOneList, stateTwoList);
        AbsoluteDate endDate = extractEndDate(stateOneList, stateTwoList);

        ephemerisSatelliteOne.setStepHandler(60, currentState -> {
            SpacecraftState state = ephemerisSatelliteTwo.propagate(currentState.getDate());
            Vector3D positionDifference = currentState.getPosition().subtract(state.getPosition());
            if (difference.getDifference() > positionDifference.getNorm()) {
                difference.setDifference(positionDifference.getNorm());
                difference.setDate(currentState.getDate());
            }
        });
        ephemerisSatelliteOne.propagate(startDate, endDate);
        ephemerisSatelliteOne.clearStepHandlers(); //Dupa propagate pentru afisare
        System.out.println("Difference(Close Approach): " + difference.getDifference() + " meters at date: " + difference.getDate().toDate(TimeScalesFactory.getUTC()));

    }

/*
    private void estimateCollision(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {

        Ephemeris ephemerisSatelliteOne = new Ephemeris(stateOneList, 4);
        Ephemeris ephemerisSatelliteTwo = new Ephemeris(stateTwoList, 4);
        final PositionDifference difference = new PositionDifference();

        AbsoluteDate startDate = extractStartDate(stateOneList, stateTwoList);
        AbsoluteDate endDate = extractEndDate(stateOneList, stateTwoList);

        ephemerisSatelliteOne.setStepHandler(60, currentState -> {
            SpacecraftState state = ephemerisSatelliteTwo.propagate(currentState.getDate());
            Vector3D positionDifference = currentState.getPosition().subtract(state.getPosition());
            if (difference.getDifference() > positionDifference.getNorm()) {
                difference.setDifference(positionDifference.getNorm());
                difference.setDate(currentState.getDate());
            }
        });

        SpacecraftState spacecraftState = ephemerisSatelliteOne.propagate(startDate, endDate);
        System.out.println(spacecraftState.getPosition());
        ephemerisSatelliteOne.clearStepHandlers(); //Dupa propagate pentru afisare
    }
    private AbsoluteDate extractStartDate(LinkedHashSet<SpacecraftState> stateOneList, LinkedHashSet<SpacecraftState> stateTwoList) {

        Optional<SpacecraftState> spacecraftStateOne = stateOneList.stream().findFirst();
        Optional<SpacecraftState> spacecraftStateTwo = stateTwoList.stream().findFirst();
        AbsoluteDate absoluteDateOne = new AbsoluteDate();
        AbsoluteDate absoluteDateTwo = new AbsoluteDate();
        if (spacecraftStateOne.isPresent() && spacecraftStateTwo.isPresent()) {
            absoluteDateOne = spacecraftStateOne.get().getDate();
            absoluteDateTwo = spacecraftStateTwo.get().getDate();
        }
        if (absoluteDateTwo.isBefore(absoluteDateTwo)) {
            return absoluteDateTwo;
        }
        return absoluteDateOne;
    }
*/

    private AbsoluteDate extractStartDate(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {

        AbsoluteDate absoluteDateOne = new AbsoluteDate();
        AbsoluteDate absoluteDateTwo = new AbsoluteDate();
        if (!stateOneList.isEmpty() && !stateTwoList.isEmpty()) {
            absoluteDateOne = stateOneList.get(0).getDate();
            absoluteDateTwo = stateTwoList.get(0).getDate();
        }
        if (absoluteDateTwo.isBefore(absoluteDateOne)) {
            return absoluteDateTwo;
        }
        return absoluteDateOne;
    }


  /*  private AbsoluteDate extractEndDate(LinkedHashSet<SpacecraftState> stateOneList, LinkedHashSet<SpacecraftState> stateTwoList) {

        SpacecraftState lastSpacecraftOne = null;
        SpacecraftState lastSpacecraftTwo = null;

        for (SpacecraftState state : stateOneList) {
            lastSpacecraftOne = state;
        }
        for (SpacecraftState state : stateTwoList) {
            lastSpacecraftTwo = state;
        }

        AbsoluteDate absoluteDateOne = new AbsoluteDate();
        AbsoluteDate absoluteDateTwo = new AbsoluteDate();

        if (lastSpacecraftOne != null) {
            absoluteDateOne = lastSpacecraftOne.getDate();
        }
        if (lastSpacecraftTwo != null) {
            absoluteDateTwo = lastSpacecraftTwo.getDate();
        }
        if (absoluteDateTwo.isBefore(absoluteDateTwo)) {
            return absoluteDateTwo;
        }
        return absoluteDateOne;
    }
*/
    private AbsoluteDate extractEndDate(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {
        AbsoluteDate absoluteDateOne = new AbsoluteDate();
        AbsoluteDate absoluteDateTwo = new AbsoluteDate();

        if (!stateOneList.isEmpty() && !stateTwoList.isEmpty()) {
            absoluteDateOne = stateOneList.get(stateOneList.size() - 1).getDate();
            absoluteDateTwo = stateTwoList.get(stateTwoList.size() - 1).getDate();
        }
        if (absoluteDateTwo.isAfter(absoluteDateOne)) {
            return absoluteDateTwo;
        }
        return absoluteDateOne;
    }

    private Date parseDate(AbsoluteDate abslouteDate) {
        return  abslouteDate.toDate(TimeScalesFactory.getUTC());
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
