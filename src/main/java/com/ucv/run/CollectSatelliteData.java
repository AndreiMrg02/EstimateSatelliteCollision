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

public class CollectSatelliteData {
    private final InternetConnectionData connectionData;

    public CollectSatelliteData() {
        this.connectionData = getInternetConnectionData();
    }

    public InternetConnectionData getInternetConnectionData() {
        String baseURL = "https://www.space-track.org";
        String authPath = "/ajaxauth/login";
        String userName = "murguandreilicenta@gmail.com";
        String password = "SpaceTrackLicenta12341!";
        return new InternetConnectionData(baseURL, authPath, userName, password);
    }

    public Map<String, Item> extractData(String predicate, String operator, String value) {
        try {
            String descendingTCA = "TCA%20desc";
            String query = String.format("/basicspacedata/query/class/cdm_public/%s/%s%s/orderby/%s/format/xml/emptyresult/show", predicate, operator, value, descendingTCA);


            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);

            URL url = new URL(connectionData.getBaseURL() + connectionData.getAuthPath());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String input = "identity=" + connectionData.getUserName() + "&password=" + connectionData.getPassword();

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            String output;
            new InputStreamReader((conn.getInputStream()));
            BufferedReader br;
            url = new URL(connectionData.getBaseURL() + query);
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            StringBuilder xmlData = new StringBuilder();
            while ((output = br.readLine()) != null) {
                System.out.println(output);
                xmlData.append(output);
            }
            url = new URL(connectionData.getBaseURL() + "/ajaxauth/logout");
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            conn.disconnect();
            XmlParser parser = new XmlParser();
            return parser.parseItems(xmlData.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    public String loadTLEs(String query) {
        try {

            StringBuilder stringBuilder = new StringBuilder();
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);
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
                System.out.println(output);

                if (!output.equals("NO RESULTS RETURNED")) {
                   /* if (output.contains("&lt;")) {
                        String newOutput = output.replace("&lt;", "<");
                        stringBuilder.append(newOutput);
                    } else {
                        stringBuilder.append(output);
                    }*/
                    stringBuilder.append(output);
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


}
