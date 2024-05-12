package com.ucv.satellite;

import org.orekit.propagation.analytical.tle.TLE;

public class SpatialObject {
    private String name;
    private TLE tle;
    private String tca;


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
            System.err.println("Șirul nu are suficiente linii pentru a seta proprietățile.");
        }
    }

    public String getTca() {
        return tca;
    }

    public void setTca(String tca) {
        this.tca = tca;
    }
}
