package com.ucv.implementation;

import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.datamodel.xml.Item;
import com.ucv.util.XmlParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CollectSatelliteData {
    private final InternetConnectionData connectionData;
    private final Logger logger = LogManager.getLogger(CollectSatelliteData.class);

    public CollectSatelliteData(InternetConnectionData connectionData) {
        this.connectionData = connectionData;
    }


    public Map<String, Item> extractSatelliteData(String predicate, String operator, String value) {
        String descendingTCA = "TCA%20desc";
        String query = String.format("/basicspacedata/query/class/cdm_public/%s/%s%s/orderby/%s/format/xml/emptyresult/show", predicate, operator, value, descendingTCA);

        cookieInit();
        HttpsURLConnection conn = getHttpsURLConnection();

        if (conn != null && verifyConnectionResponse(conn)) {
            Map<String,Item> invalidCredentials = new HashMap<>();
            invalidCredentials.put("InvalidCredentials", new Item());
            return invalidCredentials;
        }

        String baseURL = connectionData.getBaseURL();
        StringBuilder xmlData = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(baseURL + query).openStream()))) {
            String output;
            while ((output = br.readLine()) != null) {
                xmlData.append(output);
            }
        } catch (IOException e) {
            logger.error("Error reading satellite data", e);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(baseURL + "/ajaxauth/logout").openStream()))) {
            // empty try block
        } catch (IOException e) {
            logger.error("Error during logout", e);
        } finally {
            assert conn != null;
            conn.disconnect();
        }

        XmlParser parser = new XmlParser();
        try {
            return parser.parseItems(xmlData.toString());
        } catch (Exception e) {
            logger.error("Error parsing satellite data", e);
        }

        return new HashMap<>();
    }

    private boolean verifyConnectionResponse(HttpsURLConnection connection) {
        try {
            if (connection.getResponseCode() == 401) {
                return true;
            }
        } catch (Exception ex) {
            logger.error("Error verifying connection response", ex);
            return false;
        }
        return false;
    }

    public String extractSatelliteTLEs(String query) {
        StringBuilder stringBuilder = new StringBuilder();
        cookieInit();
        HttpsURLConnection conn = null;

        try {
            conn = getHttpsURLConnection();
            if (conn == null) {
                throw new IllegalStateException("Connection could not be established.");
            }

            URL url = new URL(connectionData.getBaseURL() + query);
            // Using try-with-resources to ensure the BufferedReader is closed
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                appendReadLine(br, stringBuilder);
            }

            // Handling logout in a separate try-with-resources to ensure it occurs
            url = new URL(connectionData.getBaseURL() + "/ajaxauth/logout");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            logger.error("Error extracting satellite TLEs", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }


    private void appendReadLine(BufferedReader br, StringBuilder stringBuilder) {
        try {
            String output;
            while ((output = br.readLine()) != null) {
                if (!output.equals("NO RESULTS RETURNED")) {
                    stringBuilder.append(output);
                    stringBuilder.append("\n");
                }
            }
        } catch (Exception exception) {
            logger.error("Unexpected error occurred due to append read line", exception);
        }
    }

    private HttpsURLConnection getHttpsURLConnection() {
        try {
            URL url = new URL(connectionData.getBaseURL() + connectionData.getAuthPath());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String input = "identity=" + connectionData.getUserName() + "&password=" + connectionData.getPassword();

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            return conn;
        } catch (Exception ex) {
            logger.error("Unexpected error occurred due to connect to Space-Track", ex);
        }
        return null;
    }

    private void cookieInit() {
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
    }


}