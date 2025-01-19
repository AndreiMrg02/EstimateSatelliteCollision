package com.ucv.tle;

import com.ucv.controller.SatelliteController;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import com.ucv.implementation.ConnectionService;
import com.ucv.util.CustomAlert;
import com.ucv.util.LoggerCustom;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class TleService {
    private final CustomAlert customAlert = new CustomAlert();
    private final ConnectionService connectionService;
    private final boolean isOnline;
    private final TleFileHandler tleFileHandler;
    private final SatelliteController satelliteController;
    private final int days;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public TleService(ConnectionService connectionService,
                      SatelliteController satelliteController,
                      boolean isOnline,
                      int days,
                      LocalDate startDate,
                      LocalDate endDate) {
        this.connectionService = connectionService;
        this.isOnline = isOnline;
        this.satelliteController = satelliteController;
        this.days = days;
        this.startDate = startDate;
        this.endDate = endDate;
        this.tleFileHandler = new TleFileHandler();
    }

    public boolean downloadTLEs(Map<String, Item> listOfUniqueSatellite,
                                Map<String, String[]> tleData,
                                CollectSatelliteData collectSatelliteData,
                                StackPane informationPane) {
        ImportTle importTle;
        if (!isOnline) {
            importTle = new TleLocalImport(satelliteController, days, startDate, endDate, tleData);
            importTle.importTle(listOfUniqueSatellite);
            return true;
        } else {
            if (connectionService.hasOneHourPassedSinceLastConnection(connectionService.getConnectionData().getUserName())) {
                importTle = new TleSpaceTrackImport(connectionService, satelliteController, days, startDate, endDate, collectSatelliteData);
                importTle.importTle(listOfUniqueSatellite);
                return true;
            }
            Platform.runLater(() -> customAlert.alertWaitSpaceTrackTle(informationPane));
            return false;
        }
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
