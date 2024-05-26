package com.ucv.run;

import com.ucv.Util.XmlParser;
import com.ucv.datamodel.internet.InternetConnectionData;
import com.ucv.datamodel.xml.Item;

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

public class DownloadTLE {
    private Map<String, Item> listOfUniqueSatellite = new HashMap<>();
    private final InternetConnectionData connectionData;

    public DownloadTLE() {
        connectionData = getInternetConnectionData();
    }

    public Map<String, Item> extractSatelliteDataFromXml(String query) {
        try {

            StringBuilder xmlStringData = new StringBuilder();

            setCookieManager();

            URL url = new URL(connectionData.getBaseURL() + connectionData.getAuthPath());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String input = "identity=" + connectionData.getUserName() + "&password=" + connectionData.getPassword();

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            new InputStreamReader((conn.getInputStream()));
            BufferedReader br;
            String output;

            System.out.println("Output from Server .... \n");
            url = new URL(connectionData.getBaseURL() + query);

            br = new BufferedReader(new InputStreamReader((url.openStream())));

            while ((output = br.readLine()) != null) {
                if (output.contains("&lt;")) {
                    String newOutput = output.replace("&lt;", "<");
                    xmlStringData.append(newOutput);
                } else {
                    xmlStringData.append(output);
                }

            }
            url = new URL(connectionData.getBaseURL() + "/ajaxauth/logout");
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            XmlParser parser = new XmlParser();
            listOfUniqueSatellite = parser.parseItems(xmlStringData.toString());
            conn.disconnect();
            return listOfUniqueSatellite;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    static void setCookieManager() {
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
    }

    public InternetConnectionData getInternetConnectionData() {
        String baseURL = "https://www.space-track.org";
        String authPath = "/ajaxauth/login";
        String userName = "murguandreilicenta@gmail.com";
        String password = "SpaceTrackLicenta12341!";
        return new InternetConnectionData(baseURL, authPath, userName, password);
    }

    public String loadTLEs(String query) {
        try {

            StringBuilder stringBuilder = new StringBuilder();
            setCookieManager();
            URL url = new URL(connectionData.getBaseURL() + connectionData.getAuthPath());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String input = "identity=" + connectionData.getUserName() + "&password=" + connectionData.getPassword();

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");

            url = new URL(connectionData.getBaseURL() + query);

            br = new BufferedReader(new InputStreamReader((url.openStream())));

            while ((output = br.readLine()) != null) {
                System.out.println(output);

                if (!output.equals("NO RESULTS RETURNED")) {
                    if (output.contains("&lt;")) {
                        String newOutput = output.replace("&lt;", "<");
                        stringBuilder.append(newOutput);
                    } else {
                        stringBuilder.append(output);
                    }
                    stringBuilder.append("\n");
                }
            }

            url = new URL(connectionData.getBaseURL() + "/ajaxauth/logout");
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            conn.disconnect();

            return stringBuilder.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Map<String, Item> getListOfUniqueSatellite() {
        return listOfUniqueSatellite;
    }

}