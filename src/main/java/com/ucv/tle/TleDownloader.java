package com.ucv.tle;


import com.ucv.controller.SatelliteController;
import com.ucv.database.DBOperation;
import com.ucv.datamodel.database.ConnectionInformation;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import com.ucv.implementation.ConnectionService;

import java.util.LinkedHashMap;
import java.util.Map;

public class TleDownloader {

    private final ConnectionService connectionService;
    private final SatelliteController satelliteController;

    public TleDownloader(ConnectionService connectionService, SatelliteController satelliteController) {
        this.connectionService = connectionService;
        this.satelliteController = satelliteController;
    }

    public void extractTLEsUsingSpaceTrack(Map<String, Item> listOfUniqueSatellite, CollectSatelliteData collectSatelliteData) {
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
        TleFileHandler tleFileHandler = new TleFileHandler();
        tleFileHandler.generateTleFile(satelliteTLEs);
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
}
