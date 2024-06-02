package com.ucv.datamodel.satellite;

public class CollisionData {
    private String startDate;
    private String endDate;
    private String closeApproach;
    private String closeApproachDate;
    private String sat1Name;
    private String sat2Name;
    private String collisionProbability;

    public CollisionData(String startDate, String endDate, String closeApproach, String closeApproachDate, String sat1Name, String sat2Name, String collisionProbability) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.closeApproach = closeApproach;
        this.closeApproachDate = closeApproachDate;

        this.sat1Name = sat1Name;
        this.sat2Name = sat2Name;
        this.collisionProbability = collisionProbability;
    }


    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getCloseApproach() {
        return closeApproach;
    }

    public void setCloseApproach(String closeApproach) {
        this.closeApproach = closeApproach;
    }

    public String getCloseApproachDate() {
        return closeApproachDate;
    }

    public void setCloseApproachDate(String closeApproachDate) {
        this.closeApproachDate = closeApproachDate;
    }

    public String getSat1Name() {
        return sat1Name;
    }

    public void setSat1Name(String sat1Name) {
        this.sat1Name = sat1Name;
    }

    public String getSat2Name() {
        return sat2Name;
    }

    public void setSat2Name(String sat2Name) {
        this.sat2Name = sat2Name;
    }

    public String getCollisionProbability() {
        return collisionProbability;
    }

    public void setCollisionProbability(String collisionProbability) {
        this.collisionProbability = collisionProbability;
    }
}
