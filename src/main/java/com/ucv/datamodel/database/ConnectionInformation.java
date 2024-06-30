package com.ucv.datamodel.database;

import jakarta.persistence.*;

@Entity(name = "ConnectionInformation")
public class ConnectionInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "Username")
    private String username;
    @Column(name = "Last_Conection_Date")
    private String lastConnectionDate;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLastConnectionDate() {
        return lastConnectionDate;
    }

    public void setLastConnectionDate(String lastConnectiondate) {
        this.lastConnectionDate = lastConnectiondate;
    }
}
