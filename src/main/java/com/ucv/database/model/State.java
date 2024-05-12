package com.ucv.database.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity(name = "States")
public class State {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "Satellite_Name")
    String satName;
    @Column(name = "Date")
    Date date;
    @Column(name = "Position_X")
    Double posX;
    @Column(name = "Position_Y")
    Double posY;
    @Column(name = "Position_Z")
    Double posZ;
    @Column(name = "Velocity_X")
    Double vX;
    @Column(name = "Velocity_Y")
    Double vY;
    @Column(name = "Velocity_Z")
    Double vZ;


    public String getSatName() {
        return satName;
    }

    public void setSatName(String satName) {
        this.satName = satName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Double getPosX() {
        return posX;
    }

    public void setPosX(Double posX) {
        this.posX = posX;
    }

    public Double getPosY() {
        return posY;
    }

    public void setPosY(Double posY) {
        this.posY = posY;
    }

    public Double getPosZ() {
        return posZ;
    }

    public void setPosZ(Double posZ) {
        this.posZ = posZ;
    }

    public Double getvX() {
        return vX;
    }

    public void setvX(Double vX) {
        this.vX = vX;
    }

    public Double getvY() {
        return vY;
    }

    public void setvY(Double vY) {
        this.vY = vY;
    }

    public Double getvZ() {
        return vZ;
    }

    public void setvZ(Double vZ) {
        this.vZ = vZ;
    }
}
