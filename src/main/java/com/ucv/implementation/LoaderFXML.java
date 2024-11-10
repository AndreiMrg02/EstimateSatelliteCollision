package com.ucv.implementation;

import com.ucv.Main;
import com.ucv.controller.EarthViewController;
import com.ucv.controller.SatelliteController;
import com.ucv.controller.SatelliteInformationController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.apache.log4j.Logger;

import java.io.IOException;

public class LoaderFXML {
    private static final Logger logger = Logger.getLogger(LoaderFXML.class);
    private BorderPane tableViewPane;

    public LoaderFXML(BorderPane tableViewPane) {
        this.tableViewPane = tableViewPane;
    }

    public SatelliteInformationController loadSatelliteInformation(StackPane informationPane) {
        try {
            FXMLLoader fxmlLoaderInformation = new FXMLLoader(getClass().getResource("/views/SatelliteInformation.fxml"));
            StackPane paneWithTable = fxmlLoaderInformation.load();
            SatelliteInformationController satelliteInformationController = fxmlLoaderInformation.getController();
            informationPane.getChildren().add(paneWithTable);
            informationPane.setVisible(false);
            return satelliteInformationController;
        } catch (Exception ex) {
            logger.error("Failed to load the satellite information view.", ex);
        }
        return null;
    }

    public EarthViewController loadEarth(StackPane earthPane) {
        try {
            FXMLLoader fxmlLoaderEarth = new FXMLLoader(getClass().getResource("/views/EarthViewNou.fxml"));
            StackPane paneWithEarth = fxmlLoaderEarth.load();
            EarthViewController earthViewController = fxmlLoaderEarth.getController();
            Platform.runLater(() -> earthPane.getChildren().add(paneWithEarth));
            return earthViewController;
        } catch (Exception ex) {
            logger.error("Failed to load the Earth view.", ex);
        }
        return null;
    }

    public SatelliteController loadTableSatellite(BorderPane mainPanel, ProgressIndicator progressBar) {
        try {
            tableViewPane = (BorderPane) mainPanel.lookup("#tableViewPane");
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/views/TableSatelliteExtended.fxml"));
            BorderPane tableViewLayout = fxmlLoader.load();
            SatelliteController satelliteController = fxmlLoader.getController();
            successExtractList(tableViewLayout, progressBar);
            return satelliteController;
        } catch (IOException e) {
            logger.error("Failed to load the satellite view.", e);
        }
        return null;
    }

    private void successExtractList(BorderPane tableViewLayout, ProgressIndicator progressBar) {
        progressBar.setVisible(false);
        tableViewPane.setVisible(true);
        tableViewPane.setCenter(tableViewLayout);
    }

}
