package com.ucv.datamodel.satellite;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.util.List;

public class DisplaySatelliteModel {
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;
    private String name;
    private Ephemeris ephemeris;
    private List<SpacecraftState> spacecraftStateList;
    public DisplaySatelliteModel(AbsoluteDate startDate, AbsoluteDate endDate, String name, Ephemeris ephemeris, List<SpacecraftState> spacecraftStateList) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
        this.ephemeris = ephemeris;
        this.spacecraftStateList = spacecraftStateList;


    }

    public DisplaySatelliteModel() {
        this.startDate = new AbsoluteDate();
        this.endDate = new AbsoluteDate();
        this.ephemeris = null;
        this.name = "";
    }

    public AbsoluteDate getStartDate() {
        return startDate;
    }

    public void setStartDate(AbsoluteDate startDate) {
        this.startDate = startDate;
    }

    public AbsoluteDate getEndDate() {
        return endDate;
    }

    public void setEndDate(AbsoluteDate endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Ephemeris getEphemeris() {
        return ephemeris;
    }

    public void setEphemeris(Ephemeris ephemeris) {
        this.ephemeris = ephemeris;
    }


    public List<SpacecraftState> getSpacecraftStateList() {
        return spacecraftStateList;
    }

    public void setSpacecraftStateList(List<SpacecraftState> spacecraftStateList) {
        this.spacecraftStateList = spacecraftStateList;
    }
}
