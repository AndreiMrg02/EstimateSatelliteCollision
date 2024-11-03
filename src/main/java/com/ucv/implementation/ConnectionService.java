package com.ucv.implementation;

import com.ucv.datamodel.database.ConnectionInformation;
import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.util.LoggerCustom;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.ucv.database.DBOperation.getLastConnectionFromDB;

public class ConnectionService {
    private final InternetConnectionData connectionData;
    public ConnectionService(InternetConnectionData internetConnectionData){
        this.connectionData = internetConnectionData;
    }
    ConnectionInformation generateConnectionInfo() {
        ConnectionInformation connectionInformation = new ConnectionInformation();
        connectionInformation.setUsername(connectionData.getUserName());
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        connectionInformation.setLastConnectionDate(formattedDateTime);
        return connectionInformation;
    }

    public boolean hasOneHourPassedSinceLastConnection(String username) {
        ConnectionInformation connectionInformation = getLastConnectionFromDB(username);
        if (connectionInformation == null) {
            return true;
        }
        String lastConnectionDateStr = connectionInformation.getLastConnectionDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime lastConnectionDate = LocalDateTime.parse(lastConnectionDateStr, formatter);
        LocalDateTime currentDate = LocalDateTime.now();

        Duration duration = Duration.between(lastConnectionDate, currentDate);

        if (duration.toHours() >= 1) {
            return true;
        } else {
            long minutesLeft = 60 - duration.toMinutes();
            long secondsLeft = 3600 - duration.getSeconds();
            LoggerCustom.getInstance().logMessage(String.format("IMPORTANT:The remaining time to use this option again is: %s min and %s sec", minutesLeft, (secondsLeft % 60)));
            return false;
        }
    }

    public InternetConnectionData getConnectionData() {
        return connectionData;
    }
}
