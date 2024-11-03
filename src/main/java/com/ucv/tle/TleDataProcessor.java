package com.ucv.tle;

import com.ucv.controller.SatelliteController;
import com.ucv.datamodel.xml.Item;

import java.util.Map;

public class TleDataProcessor {

    private final SatelliteController satelliteController;

    public TleDataProcessor(SatelliteController satelliteController) {
        this.satelliteController = satelliteController;
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
        satelliteController.manageSatellites();
    }
}
