package com.ucv.handler;

import com.ucv.datamodel.satellite.CollisionData;
import com.ucv.datamodel.satellite.SpatialObject;
import com.ucv.util.FileHandler;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SatelliteFileHandler implements FileHandler {

    private final TableView<CollisionData> satelliteTable;
    private final List<String> listOfTle;
    private final Map<String, SpatialObject> spatialObjectList;

    private final Logger logger = LogManager.getLogger(SatelliteFileHandler.class);

    public SatelliteFileHandler(TableView<CollisionData> satelliteTable, List<String> listOfTle,Map<String, SpatialObject> spatialObjectList) {
        this.satelliteTable = satelliteTable;
        this.listOfTle = listOfTle;
        this.spatialObjectList = spatialObjectList;
    }


    public void generateFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the location to save TLEs");
        fileChooser.setInitialFileName("ExtractedTLE.txt");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(satelliteTable.getScene().getWindow());

        if (file != null) {
            writeToFile(file.getParent(), file.getName());
        }
    }
    /*
     * Write Tle in a file
     */
    private void writeToFile(String directoryPath, String fileName) {
        File file = new File(directoryPath, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            for (String tle : listOfTle) {
                writer.write(tle + System.lineSeparator());
            }
        } catch (IOException e) {
            logger.error(String.format("Failed to write to file: %s", file.getAbsolutePath()), e);
        }
    }
    public void addTLEsToTextFile() {
        listOfTle.addAll(spatialObjectList.values().stream().flatMap(spatialObject -> {
            String tle = spatialObject.getTle();
            if (tle != null) {
                return Stream.of(tle);
            } else {
                return Stream.of();
            }
        }).collect(Collectors.toList()));
    }
}
