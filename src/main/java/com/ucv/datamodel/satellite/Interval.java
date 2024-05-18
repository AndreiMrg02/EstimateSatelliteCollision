package com.ucv.datamodel.satellite;

import org.orekit.time.AbsoluteDate;

public class Interval {

    private AbsoluteDate startDate;
    private AbsoluteDate endDate;
    private int count;

    public Interval(AbsoluteDate startDate, AbsoluteDate endDate, int count) {
        super();
        this.startDate = startDate;
        this.endDate = endDate;
        this.count = count;
    }

    public AbsoluteDate getStartDate() {
        return startDate;
    }
    public AbsoluteDate getEndDate() {
        return endDate;
    }
    public int getCount() {
        return count;
    }

    public void setStartDate(AbsoluteDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(AbsoluteDate endDate) {
        this.endDate = endDate;
    }

    public void setCount(int count) {
        this.count = count;
    }



}