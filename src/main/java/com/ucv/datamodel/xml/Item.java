package com.ucv.datamodel.xml;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "item")
@XmlAccessorType(XmlAccessType.FIELD)
public class Item {
    @XmlElement(name = "CDM_ID")
    private String cdmId;
    @XmlElement(name = "CREATED")
    private String created;
    @XmlElement(name = "EMERGENCY_REPORTABLE")
    private String emergencyReportable;
    @XmlElement(name = "TCA")
    private String tca;
    @XmlElement(name = "MIN_RNG")
    private String minRng;
    @XmlElement(name = "PC")
    private String pc;
    @XmlElement(name = "SAT_1_ID")
    private String sat1Id;
    @XmlElement(name = "SAT_1_NAME")
    private String sat1Name;
    @XmlElement(name = "SAT1_OBJECT_TYPE")
    private String sat1ObjectType;
    @XmlElement(name = "SAT1_RCS")
    private String sat1Rcs;
    @XmlElement(name = "SAT_1_EXCL_VOL")
    private String sat1ExclVol;
    @XmlElement(name = "SAT_2_ID")
    private String sat2Id;
    @XmlElement(name = "SAT_2_NAME")
    private String sat2Name;
    @XmlElement(name = "SAT2_OBJECT_TYPE")
    private String sat2ObjectType;
    @XmlElement(name = "SAT2_RCS")
    private String sat2Rcs;
    @XmlElement(name = "SAT_2_EXCL_VOL")
    private String sat2ExclVol;


    public String getTca() {
        return tca;
    }

    public String getSat1Id() {
        return sat1Id;
    }
    public String getSat1Name() {
        return sat1Name;
    }


    public String getSat2Id() {
        return sat2Id;
    }

    public String getSat2Name() {
        return sat2Name;
    }


}
