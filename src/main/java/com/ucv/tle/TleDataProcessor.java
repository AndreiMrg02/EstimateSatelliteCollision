package com.ucv.tle;

import com.ucv.controller.SatelliteController;
import com.ucv.datamodel.xml.Item;

import java.time.LocalDate;
import java.util.Map;

public class TleDataProcessor {

    private final SatelliteController satelliteController;
    private final int days;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public TleDataProcessor(SatelliteController satelliteController, int days, LocalDate startDate, LocalDate endDate) {
        this.satelliteController = satelliteController;
        this.days = days;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void extractTLEsUsingLocalFile(Map<String, Item> listOfUniqueSatellite, Map<String, String[]> tleData) {
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
        satelliteController.manageSatellites(days, startDate, endDate);
    }
}
