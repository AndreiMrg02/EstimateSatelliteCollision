package com.ucv.datamodel.satellite;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.orekit.propagation.analytical.tle.TLE;

public class SpatialObject {
    private String name;
    private TLE tle;
    private String tca;
    private static final Logger logger = LogManager.getLogger(SpatialObject.class);


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TLE getTle() {
        return tle;
    }

    public void setTle(TLE tle) {
        this.tle = tle;
    }

    public void setPropertiesFromString(String tle, String satName) {

        String[] lines = tle.split("\n");

        if (lines.length >= 2) {
            this.setName(satName);
            String tleLine1 = lines[0].trim();
            String tleLine2 = lines[1].trim();


            TLE tleObject = new TLE(tleLine1, tleLine2);
            this.setTle(tleObject);
        } else {
            logger.error("The string does not have enough lines to set the properties.");
        }
    }

    public String getTca() {
        return tca;
    }

    public void setTca(String tca) {
        this.tca = tca;
    }
}
