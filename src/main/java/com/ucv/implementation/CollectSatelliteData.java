package com.ucv.implementation;

import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.datamodel.xml.Item;
import com.ucv.util.XmlParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
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
        try {
            String descendingTCA = "TCA%20desc";
            String query = String.format("/basicspacedata/query/class/cdm_public/%s/%s%s/orderby/%s/format/xml/emptyresult/show", predicate, operator, value, descendingTCA);

            cookieInit();
            HttpsURLConnection conn = getHttpsURLConnection();
            URL url;

            String output;
            if (verifyConnectionResponse(conn)) {
                return null;
            }
            new InputStreamReader((conn.getInputStream()));
            BufferedReader br;
            url = new URL(connectionData.getBaseURL() + query);
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            StringBuilder xmlData = new StringBuilder();
            while ((output = br.readLine()) != null) {
                xmlData.append(output);
            }
            url = new URL(connectionData.getBaseURL() + "/ajaxauth/logout");
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            conn.disconnect();
            XmlParser parser = new XmlParser();
            return parser.parseItems(xmlData.toString());

        } catch (Exception e) {
            logger.error("Error extracting satellite data", e);
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
        }
        return false;
    }

    public String extractSatelliteTLEs(String query) {
        try {

            StringBuilder stringBuilder = new StringBuilder();
            cookieInit();
            HttpsURLConnection conn = getHttpsURLConnection();
            URL url;
            if (conn != null) {
                new InputStreamReader((conn.getInputStream()));
            }
            BufferedReader br;

            url = new URL(connectionData.getBaseURL() + query);
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            appendReadLine(br, stringBuilder);
            url = new URL(connectionData.getBaseURL() + "/ajaxauth/logout");
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            assert conn != null;
            conn.disconnect();

            return stringBuilder.toString();

        } catch (Exception e) {
            logger.error("Error extracting satellite TLEs", e);
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