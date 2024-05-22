package com.ucv.controller;


import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.PositionDifference;
import com.ucv.datamodel.satellite.SpatialObject;
import com.ucv.datamodel.satellite.SpatialObjectTableModel;
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
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.orbits.Orbit;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.ucv.database.DBManager.getStatesAllSatelliteName;
import static com.ucv.database.DBManager.getStatesBySatelliteName;


public class SatelliteExtendController implements Initializable {
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private Map<String, Item> listOfUniqueSatellite = new HashMap<>();
    private ArrayList<String> listOfTle;
    private List<SpatialObject> spatialObjectList;
    private Set<DisplaySatelliteModel> stringDisplaySatelliteModelMap;
    @FXML
    private Button viewTLEButton;
    private List<DisplaySatelliteModel> twoSatellitesSelected;
    @FXML
    private Button extractTleToFileButton;
    @FXML
    private BorderPane satellitePane;
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

    private final List<String> satName = new ArrayList<>();
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
        // Initialize your lists and sets
        spatialObjectList = new ArrayList<>();
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
            System.out.println("Au fost adaugate starile pentru satelitul: "+ spatialObject.getName() + "cu TLE-ul: "+ spatialObject.getTle());
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

    // Check by
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
        satelliteTable.setItems(FXCollections.observableList(spatialObjectTableModels));
        satelliteTable.refresh();
    }


    public static String formatAbsoluteDate(AbsoluteDate date) {
        // Assuming AbsoluteDate can be directly converted to java.util.Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date javaDate = date.toDate(TimeScalesFactory.getUTC());
        return dateFormat.format(javaDate);
    }
    private void estimateCollision(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList, String entryOne, String entryTwo) {
        try {
            // Extrage datele de început și de sfârșit
            AbsoluteDate startDate = extractStartDate(stateOneList, stateTwoList);
            AbsoluteDate endDate = extractEndDate(stateOneList, stateTwoList);

            if (startDate == null || endDate == null) {
                System.out.println("Nu exista data comuna intre satelitii: " + entryOne + " si " + entryTwo);
                return;
            }

            Ephemeris ephemerisSatelliteOne = new Ephemeris(stateOneList, 4);
            Ephemeris ephemerisSatelliteTwo = new Ephemeris(stateTwoList, 4);

            final PositionDifference closestApproach = new PositionDifference();

            // Setează handler pentru propagarea ephemeris
            ephemerisSatelliteOne.setStepHandler(60, currentState -> {
                SpacecraftState stateTwo = ephemerisSatelliteTwo.propagate(currentState.getDate());
                Vector3D positionDifference = currentState.getPosition().subtract(stateTwo.getPosition());
                double distance = positionDifference.getNorm();

                // Verifică dacă poziția este validă (de exemplu, altitudinea este mai mare de 0)
                if (currentState.getPVCoordinates().getPosition().getNorm() > 6371 &&  // Radius of Earth in km
                        stateTwo.getPVCoordinates().getPosition().getNorm() > 6371) {

                    if (closestApproach.getDifference() > distance) {
                        closestApproach.setDifference(distance);
                        closestApproach.setDate(currentState.getDate());
                    }
                }
            });

            ephemerisSatelliteOne.propagate(startDate, endDate);
            ephemerisSatelliteOne.clearStepHandlers();

            if (!satName.contains(entryOne)) {
                stringDisplaySatelliteModelMap.add(new DisplaySatelliteModel(startDate, endDate, entryOne, ephemerisSatelliteOne, stateOneList, closestApproach.getDate()));
                satName.add(entryOne);
            }
            if (!satName.contains(entryTwo)) {
                stringDisplaySatelliteModelMap.add(new DisplaySatelliteModel(startDate, endDate, entryTwo, ephemerisSatelliteTwo, stateTwoList, closestApproach.getDate()));
                satName.add(entryTwo);
            }

            final double threshold = 1000; // Set your threshold value here
            double collisionProbability = estimateCollisionProbability(closestApproach.getDifference(), threshold);

            System.out.println("Closest approach between Satellites: " + entryOne + " and " + entryTwo + " is: " + closestApproach.getDifference() + " meters at date: " + closestApproach.getDate().toDate(TimeScalesFactory.getUTC()));
            System.out.println("Collision probability is: " + String.format("%.10f%%", collisionProbability));
            String collisionFormat = String.format("%.10f%%", collisionProbability);

            // Verifică dacă poziția apropiată este validă înainte de a adăuga în tabel
            if (closestApproach.getDifference() > 0) {
                SpatialObjectTableModel spatialObjectTableModel = new SpatialObjectTableModel(
                        formatAbsoluteDate(startDate), formatAbsoluteDate(endDate),
                        Double.toString(closestApproach.getDifference()), formatAbsoluteDate(closestApproach.getDate()),
                        entryOne, entryTwo, collisionFormat);

                spatialObjectTableModels.add(spatialObjectTableModel);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

/*    private void estimateCollision(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList, String entryOne, String entryTwo) {
        try {

            // Extrage datele de început și de sfârșit
            AbsoluteDate startDate = extractStartDate(stateOneList, stateTwoList);
            AbsoluteDate endDate = extractEndDate(stateOneList, stateTwoList);

            if(startDate == null || endDate == null){
                System.out.println("Nu exista data comuna intre satelitii: " + entryOne +  " si " + entryTwo);
                return;
            }

            Ephemeris ephemerisSatelliteOne = new Ephemeris(stateOneList, 4);
            Ephemeris ephemerisSatelliteTwo = new Ephemeris(stateTwoList, 4);

            final PositionDifference closestApproach = new PositionDifference();

            // Setează handler pentru propagarea ephemeris
            ephemerisSatelliteOne.setStepHandler(60, currentState -> {
                    SpacecraftState stateTwo = ephemerisSatelliteTwo.propagate(currentState.getDate());
                    Vector3D positionDifference = currentState.getPosition().subtract(stateTwo.getPosition());
                    double distance = positionDifference.getNorm();
                    if (closestApproach.getDifference() > distance) {
                        closestApproach.setDifference(distance);
                        closestApproach.setDate(currentState.getDate());
                    }

            });

                ephemerisSatelliteOne.propagate(startDate, endDate);
                ephemerisSatelliteOne.clearStepHandlers();

            if (!satName.contains(entryOne)) {
                stringDisplaySatelliteModelMap.add(new DisplaySatelliteModel(startDate, endDate, entryOne, ephemerisSatelliteOne, stateOneList, closestApproach.getDate()));
                satName.add(entryOne);
            }
            if (!satName.contains(entryTwo)) {
                stringDisplaySatelliteModelMap.add(new DisplaySatelliteModel(startDate, endDate, entryTwo, ephemerisSatelliteTwo, stateTwoList, closestApproach.getDate()));
                satName.add(entryTwo);
            }

            final double threshold = 1000; // Set your threshold value here
            double collisionProbability = estimateCollisionProbability(closestApproach.getDifference(), threshold);

            System.out.println("Closest approach between Satellites: " + entryOne + " and " + entryTwo + " is: " + closestApproach.getDifference() + " meters at date: " + closestApproach.getDate().toDate(TimeScalesFactory.getUTC()));
            System.out.println("Collision probability is: " + String.format("%.10f%%", collisionProbability));
            String collisionFormat = String.format("%.10f%%", collisionProbability);
            SpatialObjectTableModel spatialObjectTableModel = new SpatialObjectTableModel(formatAbsoluteDate(startDate), formatAbsoluteDate(endDate),
                    Double.toString(closestApproach.getDifference()), formatAbsoluteDate(closestApproach.getDate()),
                    entryOne, entryTwo, collisionFormat);

            spatialObjectTableModels.add(spatialObjectTableModel);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }*/

    private double estimateCollisionProbability(double closestApproachDistance, double threshold) {
        double scale = 0.2;  // Factorul de scalare pentru a ajusta sensibilitatea
        double ratio = (closestApproachDistance / threshold);
        double collisionProbability = Math.exp(-scale * ratio);  // Aplică factorul de scalare în exponent
        return collisionProbability * 100;  // Converteste în procente
    }


    private AbsoluteDate extractStartDate(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {
        if (stateOneList.isEmpty() || stateTwoList.isEmpty()) {
            return null;
        }
        for (SpacecraftState stateOne : stateOneList) {
            for (SpacecraftState stateTwo : stateTwoList) {
                if (stateOne.getDate().equals(stateTwo.getDate())) {
                    return stateOne.getDate();
                }
            }
        }

        return null;
    }

    private AbsoluteDate extractEndDate(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {
        if (stateOneList.isEmpty() || stateTwoList.isEmpty()) {
            return null;
        }
        for (int i = stateOneList.size() - 1; i >= 0; i--) {
            for (int j = stateTwoList.size() - 1; j >= 0; j--) {
                if (stateOneList.get(i).getDate().equals(stateTwoList.get(j).getDate())) {
                    return stateOneList.get(i).getDate();
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
