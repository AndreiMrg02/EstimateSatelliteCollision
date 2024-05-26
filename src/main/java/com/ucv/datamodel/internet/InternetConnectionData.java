package com.ucv.datamodel.internet;

public class InternetConnectionData {
    private final String baseURL;
    private final String authPath;
    private final String userName;
    private final String password;

    public InternetConnectionData(String baseURL, String authPath, String userName, String password) {
        this.baseURL = baseURL;
        this.authPath = authPath;
        this.userName = userName;
        this.password = password;
    }

    public String getBaseURL() {
        return baseURL;
    }


    public String getAuthPath() {
        return authPath;
    }


    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }


}
