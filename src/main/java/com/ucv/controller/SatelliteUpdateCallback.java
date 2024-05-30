package com.ucv.controller;

// SatelliteUpdateCallback.java
public interface SatelliteUpdateCallback {
    void updateSatelliteData(String satelliteName, double latitude, double longitude, double altitude, double speed);
}
