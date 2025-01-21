package com.ucv.tle;

import com.ucv.util.FileHandler;
import com.ucv.util.LoggerCustom;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.apache.log4j.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TleFileHandler implements FileHandler {
    private static final Logger logger = Logger.getLogger(TleFileHandler.class);
    private Map<String, String> satelliteTLEs;

    public TleFileHandler(Map<String, String> satelliteTLEs) {
        this.satelliteTLEs = satelliteTLEs;
    }

    public Map<String, String[]> readTLEFile(String filePath) {
        Map<String, String[]> tleMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                readTLEProcess(line, reader, tleMap);
            }
        } catch (IOException e) {
            logger.error("The TLE file contains corrupt data", e);
        }
        return tleMap;
    }

    private void readTLEProcess(String line, BufferedReader reader, Map<String, String[]> tleMap) throws IOException {
        if (line.startsWith("1 ")) {
            String line2 = reader.readLine();
            if (line2 != null && line2.startsWith("2 ")) {
                String satelliteId = line.substring(2, 7).trim();
                String[] tle = {line, line2};
                tleMap.put(satelliteId, tle);
            }
        }
    }

    public Map<String, String[]> chooseTleFile(BorderPane mainPanel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select TLE File");
        Map<String, String[]> tleData = new LinkedHashMap<>();
        File tleFile = fileChooser.showOpenDialog(mainPanel.getScene().getWindow());
        if (tleFile != null) {
            tleData = readTLEFile(tleFile.getAbsolutePath());
        } else {
            logger.error("File selection cancelled by user.");
        }
        return tleData;
    }

    public void generateFile() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "tle_output_" + timestamp + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, String> entry : satelliteTLEs.entrySet()) {
                writer.write(entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("Unexpected exception occurred during generating TLE output file: " + e.getMessage());
        }
        LoggerCustom.getInstance().logMessage("Generated TLE output file: " + fileName);
    }
}
