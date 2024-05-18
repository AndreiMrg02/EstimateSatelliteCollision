package com.ucv.datamodel.xml;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "item")
@XmlAccessorType(XmlAccessType.FIELD)
public class Item {
    @XmlElement(name = "CDM_ID")
    String cdmId;
    @XmlElement(name = "CREATED")
    String created;
    @XmlElement(name = "EMERGENCY_REPORTABLE")
    String emergencyReportable;
    @XmlElement(name = "TCA")
    String tca;
    @XmlElement(name = "MIN_RNG")
    String minRng;
    @XmlElement(name = "PC")
    String pc;
    @XmlElement(name = "SAT_1_ID")
    String sat1Id;
    @XmlElement(name = "SAT_1_NAME")
    String sat1Name;
    @XmlElement(name = "SAT1_OBJECT_TYPE")
    String sat1ObjectType;
    @XmlElement(name = "SAT1_RCS")
    String sat1Rcs;
    @XmlElement(name = "SAT_1_EXCL_VOL")
    String  sat1ExclVol;
    @XmlElement(name = "SAT_2_ID")
    String sat2Id;
    @XmlElement(name = "SAT_2_NAME")
    String sat2Name;
    @XmlElement(name = "SAT2_OBJECT_TYPE")
    String sat2ObjectType;
    @XmlElement(name = "SAT2_RCS")
    String sat2Rcs;
    @XmlElement(name = "SAT_2_EXCL_VOL")
    String sat2ExclVol;

    public String getCdmId() {
        return cdmId;
    }

    public void setCdmId(String cdmId) {
        this.cdmId = cdmId;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getEmergencyReportable() {
        return emergencyReportable;
    }

    public void setEmergencyReportable(String emergencyReportable) {
        this.emergencyReportable = emergencyReportable;
    }

    public String getTca() {
        return tca;
    }

    public void setTca(String tca) {
        this.tca = tca;
    }

    public String getMinRng() {
        return minRng;
    }

    public void setMinRng(String minRng) {
        this.minRng = minRng;
    }

    public String getPc() {
        return pc;
    }

    public void setPc(String pc) {
        this.pc = pc;
    }

    public String getSat1Id() {
        return sat1Id;
    }

    public void setSat1Id(String sat1Id) {
        this.sat1Id = sat1Id;
    }

    public String getSat1Name() {
        return sat1Name;
    }

    public void setSat1Name(String sat1Name) {
        this.sat1Name = sat1Name;
    }

    public String getSat1ObjectType() {
        return sat1ObjectType;
    }

    public void setSat1ObjectType(String sat1ObjectType) {
        this.sat1ObjectType = sat1ObjectType;
    }

    public String getSat1Rcs() {
        return sat1Rcs;
    }

    public void setSat1Rcs(String sat1Rcs) {
        this.sat1Rcs = sat1Rcs;
    }

    public String getSat1ExclVol() {
        return sat1ExclVol;
    }

    public void setSat1ExclVol(String sat1ExclVol) {
        this.sat1ExclVol = sat1ExclVol;
    }

    public String getSat2Id() {
        return sat2Id;
    }

    public void setSat2Id(String sat2Id) {
        this.sat2Id = sat2Id;
    }

    public String getSat2Name() {
        return sat2Name;
    }

    public void setSat2Name(String sat2Name) {
        this.sat2Name = sat2Name;
    }

    public String getSat2ObjectType() {
        return sat2ObjectType;
    }

    public void setSat2ObjectType(String sat2ObjectType) {
        this.sat2ObjectType = sat2ObjectType;
    }

    public String getSat2Rcs() {
        return sat2Rcs;
    }

    public void setSat2Rcs(String sat2Rcs) {
        this.sat2Rcs = sat2Rcs;
    }

    public String getSat2ExclVol() {
        return sat2ExclVol;
    }

    public void setSat2ExclVol(String sat2ExclVol) {
        this.sat2ExclVol = sat2ExclVol;
    }
}
