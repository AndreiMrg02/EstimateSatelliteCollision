package com.ucv.satellite;

import org.orekit.time.AbsoluteDate;

public class PositionDifference {
   private double difference = Double.MAX_VALUE;
   private AbsoluteDate date;

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    public AbsoluteDate getDate() {
        return date;
    }

    public void setDate(AbsoluteDate date) {
        this.date = date;
    }
}
