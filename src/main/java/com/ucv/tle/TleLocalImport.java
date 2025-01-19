package com.ucv.tle;

import com.ucv.controller.SatelliteController;
import com.ucv.datamodel.xml.Item;

import java.time.LocalDate;
import java.util.Map;

public class TleLocalImport extends ImportTle {

    private final Map<String, String[]> tleData;

    public TleLocalImport(SatelliteController satelliteController, int days, LocalDate startDate, LocalDate endDate, Map<String,String[]> tleData) {
        super(satelliteController, days, startDate, endDate);
        this.tleData = tleData;
    }

    @Override
    protected void importTle(Map<String, Item> listOfUniqueSatellite) {
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
