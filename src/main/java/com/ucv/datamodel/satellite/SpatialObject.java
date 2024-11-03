package com.ucv.datamodel.satellite;

public class SpatialObject {
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
