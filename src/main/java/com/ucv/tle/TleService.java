package com.ucv.tle;

import com.ucv.controller.SatelliteController;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import com.ucv.implementation.ConnectionService;
import com.ucv.util.CustomAlert;
import com.ucv.util.LoggerCustom;
import javafx.application.Platform;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class TleService {
    private final CustomAlert customAlert = new CustomAlert();
    private final ConnectionService connectionService;
    private final RadioButton localTleRadio;

    private final TleFileHandler tleFileHandler;
    private final TleDownloader tleDownloader;
    private final TleDataProcessor tleDataProcessor;

    public TleService(ConnectionService connectionService, SatelliteController satelliteController, RadioButton localTleRadio, int days, LocalDate startDate, LocalDate endDate) {
        this.connectionService = connectionService;
        this.localTleRadio = localTleRadio;
        this.tleFileHandler = new TleFileHandler();
        this.tleDownloader = new TleDownloader(connectionService, satelliteController, days,startDate,endDate);
        this.tleDataProcessor = new TleDataProcessor(satelliteController,days,startDate,endDate);
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
        tleDataProcessor.extractTLEsUsingLocalFile(listOfUniqueSatellite, tleData);
    }

    private boolean handleSpaceTrackTleSelection(Map<String, Item> listOfUniqueSatellite, CollectSatelliteData collectSatelliteData, StackPane informationPane) {
        if (connectionService.hasOneHourPassedSinceLastConnection(connectionService.getConnectionData().getUserName())) {
            tleDownloader.extractTLEsUsingSpaceTrack(listOfUniqueSatellite, collectSatelliteData);
            return true;
        }
        Platform.runLater(() -> customAlert.alertWaitSpaceTrackTle(informationPane));
        return false;
    }

    public Map<String, String[]> getTleData(BorderPane mainPanel) {
        Map<String, String[]> tleData = tleFileHandler.chooseTleFile(mainPanel);
        if (tleData.isEmpty()) {
            LoggerCustom.getInstance().logMessage("WARNING: TLEs file is empty or you didn't choose a file");
            LoggerCustom.getInstance().logMessage("WARNING: Please verify if you chose a file that contains only TLEs");
            return new LinkedHashMap<>();
        }
        return tleData;
    }
}
