package com.ucv.tle;

import com.ucv.datamodel.xml.Item;

import java.util.Map;

public interface TleImporterStrategy {
    public void importTle(Map<String, Item> listOfUniqueSatellite);

}
