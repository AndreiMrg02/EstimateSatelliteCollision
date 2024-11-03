package com.ucv.implementation;

import com.ucv.controller.SatelliteController;
import com.ucv.database.DBOperation;
import com.ucv.datamodel.database.ConnectionInformation;
import com.ucv.datamodel.xml.Item;
import com.ucv.util.CustomAlert;
import com.ucv.util.LoggerCustom;
import javafx.application.Platform;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.apache.log4j.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TleService {
    private static final Logger logger = Logger.getLogger(TleService.class);
    private final CustomAlert customAlert = new CustomAlert();
    private final ConnectionService connectionService;

    private final SatelliteController satelliteController;
    private final RadioButton localTleRadio;

    public TleService(ConnectionService connectionService, SatelliteController satelliteController, RadioButton localTleRadio) {

        this.connectionService = connectionService;
        this.satelliteController = satelliteController;
        this.localTleRadio = localTleRadio;
    }

    public boolean downloadTLEs(Map<String, Item> listOfUniqueSatellite, Map<String, String[]> tleData, CollectSatelliteData collectSatelliteData,
                                RadioButton spaceTrackTleRadio, StackPane informationPane) {
        if (isLocalTleSelected()) {
            handleLocalTleSelection(listOfUniqueSatellite, tleData);
            return true;
        }
        if (isSpaceTrackTleSelected(spaceTrackTleRadio)) {
            return handleSpaceTrackTleSelection(listOfUniqueSatellite, collectSatelliteData, informationPane);
        }
        return false;
    }

    private boolean isLocalTleSelected() {
        return localTleRadio.isSelected();
    }

    private boolean isSpaceTrackTleSelected(RadioButton spaceTrackTleRadio) {
        return spaceTrackTleRadio.isSelected();
    }

    private void handleLocalTleSelection(Map<String, Item> listOfUniqueSatellite, Map<String, String[]> tleData) {
        extractTLEsUsingLocalFile(listOfUniqueSatellite, tleData);
    }

    private boolean handleSpaceTrackTleSelection(Map<String, Item> listOfUniqueSatellite, CollectSatelliteData collectSatelliteData, StackPane informationPane) {
        if (connectionService.hasOneHourPassedSinceLastConnection(connectionService.getConnectionData().getUserName())) {
            extractTLEsUsingSpaceTrack(listOfUniqueSatellite, collectSatelliteData);
            return true;
        }
        Platform.runLater(() -> customAlert.alertWaitSpaceTrackTle(informationPane));
        return false;
    }

    public Map<String, String[]> readTLEFile(String filePath) {
        Map<String, String[]> tleMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                readTLEProcess(line, reader, tleMap);
            }
        } catch (IOException e) {
            logger.error("The TLE file contains corrupt data", e);
        }
        return tleMap;
    }

    private void readTLEProcess(String line, BufferedReader reader, Map<String, String[]> tleMap) throws IOException {
        if (line.startsWith("1 ")) {
            String line2 = reader.readLine();
            if (line2 != null && line2.startsWith("2 ")) {
                String satelliteId = line.substring(2, 7).trim();
                String[] tle = {line, line2};
                tleMap.put(satelliteId, tle);
            }
        }
    }

    public Map<String, String[]> chooseTleFile(BorderPane mainPanel) {
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

    public Map<String, String[]> getTleData(BorderPane mainPanel) {

        Map<String, String[]> tleData = chooseTleFile(mainPanel);
        if (tleData.isEmpty()) {
            LoggerCustom.getInstance().logMessage("WARNING: TLEs file is empty or you didn't choose a file");
            LoggerCustom.getInstance().logMessage("WARNING: Please verify if you chose a file that contains only TLEs");
            return new LinkedHashMap<>();
        }
        return tleData;

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

    private void extractTLEsUsingSpaceTrack(Map<String, Item> listOfUniqueSatellite, CollectSatelliteData collectSatelliteData) {

        ConnectionInformation connectionInformation = connectionService.generateConnectionInfo();
        DBOperation.saveLastConnectionToDB(connectionInformation);
        String queryToDownloadAllTLEs = "/basicspacedata/query/class/gp/decay_date/null-val/epoch/%3Enow-30/orderby/norad_cat_id/format/tle";
        String allTles = collectSatelliteData.extractSatelliteTLEs(queryToDownloadAllTLEs);
        String[] lines = allTles.split("\n");
        Map<String, String> satelliteTLEs = new LinkedHashMap<>();
        collectTleLineByLine(lines, satelliteTLEs);
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

    private void collectTleLineByLine(String[] lines, Map<String, String> satelliteTLEs) {
        for (int i = 0; i < lines.length; i += 2) {
            if (i + 1 < lines.length) {
                String line1 = lines[i];
                String line2 = lines[i + 1];
                String satelliteId = line1.substring(2, 7).trim();
                String tle = line1 + "\n" + line2;
                satelliteTLEs.put(satelliteId, tle);
            }
        }
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
}
