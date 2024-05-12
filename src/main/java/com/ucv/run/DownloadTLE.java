package com.ucv.run;

import com.ucv.xml.model.Item;
import com.ucv.xml.model.Xml;


import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DownloadTLE {
private  Map<String, Item> listOfUniqueSatellite = new HashMap<>();
    public Map<String, Item> loadSatellite(String query) {
        try {

            String baseURL = "https://www.space-track.org";
            String authPath = "/ajaxauth/login";
            String userName = "murguandreilicenta@gmail.com";
            String password = "SpaceTrackLicenta12341!";
            StringBuilder stringBuilder = new StringBuilder();

            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);

            URL url = new URL(baseURL + authPath);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String input = "identity=" + userName + "&password=" + password;

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;

            System.out.println("Output from Server .... \n");
            url = new URL(baseURL + query);

            br = new BufferedReader(new InputStreamReader((url.openStream())));

            while ((output = br.readLine()) != null) {
              if(output.contains("&lt;")){
                  String newOutput = output.replace("&lt;", "<");
                  stringBuilder.append(newOutput);
              }else{
                  stringBuilder.append(output);
              }

            }

            url = new URL(baseURL + "/ajaxauth/logout");
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            JAXBContext jaxbContext = JAXBContext.newInstance(Xml.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            Xml generatedXML = (Xml) unmarshaller.unmarshal(new StringReader(stringBuilder.toString()));

            if (generatedXML == null || generatedXML.getItems() == null || generatedXML.getItems().isEmpty()) {

                return new HashMap<>();
            }
            for (Item item : generatedXML.getItems()) {
                listOfUniqueSatellite.put(item.getTca(), item);
            }
            conn.disconnect();
            return listOfUniqueSatellite;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String loadTleSatellite(String query) {
        try {
            ArrayList<String> listOfTle = new ArrayList<>();
            String baseURL = "https://www.space-track.org";
            String authPath = "/ajaxauth/login";
            String userName = "murguandreilicenta@gmail.com";
            String password = "SpaceTrackLicenta12341!";
            StringBuilder stringBuilder = new StringBuilder();

            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);

            URL url = new URL(baseURL + authPath);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String input = "identity=" + userName + "&password=" + password;

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");

            url = new URL(baseURL + query);

            br = new BufferedReader(new InputStreamReader((url.openStream())));

            while ((output = br.readLine()) != null) {
                System.out.println(output);

                if (!output.equals("NO RESULTS RETURNED")) {
                    if(output.contains("&lt;")){
                        String newOutput = output.replace("&lt;", "<");
                        stringBuilder.append(newOutput);
                    }else{
                        stringBuilder.append(output);
                    }
                    stringBuilder.append("\n");
                }
            }

            url = new URL(baseURL + "/ajaxauth/logout");
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
