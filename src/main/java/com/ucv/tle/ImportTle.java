package com.ucv.tle;

import com.ucv.controller.SatelliteController;
import com.ucv.datamodel.xml.Item;

import java.time.LocalDate;
import java.util.Map;

public abstract class ImportTle {
    protected final SatelliteController satelliteController;
    protected final int days;
    protected final LocalDate startDate;
    protected final LocalDate endDate;

    protected ImportTle(SatelliteController satelliteController, int days, LocalDate startDate, LocalDate endDate) {
        this.satelliteController = satelliteController;
        this.days = days;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    protected abstract void importTle(Map<String, Item> listOfUniqueSatellite);
}
