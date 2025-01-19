package com.ucv.tle;

import com.ucv.controller.SatelliteController;
import com.ucv.database.DBOperation;
import com.ucv.datamodel.database.ConnectionInformation;
import com.ucv.datamodel.xml.Item;
import com.ucv.implementation.CollectSatelliteData;
import com.ucv.implementation.ConnectionService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class TleSpaceTrackImport extends ImportTle {

    private final ConnectionService connectionService;
    private final CollectSatelliteData collectSatelliteData;

    public TleSpaceTrackImport(ConnectionService connectionService, SatelliteController satelliteController,
                               int days, LocalDate startDate, LocalDate endDate,
                               CollectSatelliteData collectSatelliteData) {
        super(satelliteController, days, startDate, endDate);
        this.connectionService = connectionService;
        this.collectSatelliteData = collectSatelliteData;
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

    @Override
    protected void importTle(Map<String, Item> listOfUniqueSatellite) {
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
        satelliteController.manageSatellites(days, startDate, endDate);
    }
}
