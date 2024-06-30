package com.ucv.datamodel.satellite;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpatialObject {
    private static final Logger logger = LogManager.getLogger(SpatialObject.class);
    private String name;
    private String tle;
    private String tca;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTle() {
        return tle;
    }

    public void setTle(String tle) {
        this.tle = tle;
    }

    public String getTca() {
        return tca;
    }

    public void setTca(String tca) {
        this.tca = tca;
    }
}
