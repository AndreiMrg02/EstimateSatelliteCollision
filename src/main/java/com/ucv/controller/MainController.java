package com.ucv.controller;

import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.Interval;
import com.ucv.datamodel.satellite.SatelliteEventHandler;
import com.ucv.datamodel.xml.Item;
import com.ucv.run.DownloadTLE;
import com.ucv.run.Main;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.SurfaceCircle;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import gov.nasa.worldwind.util.Logging;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.ucv.Util.HibernateUtil.closeSession;
import static com.ucv.Util.UtilConstant.*;
import static com.ucv.controller.EarthController.wwd;


public class MainController implements Initializable {

    @FXML
    private BorderPane mainPanel;
    @FXML
    private StackPane earthPane;

    @FXML
    private BorderPane tableViewPane;
    @FXML
    private ToggleButton displayEarthButton;
    @FXML
    private ProgressIndicator progressBar;
    @FXML
    private Button extractDataButton;
    @FXML
    private ChoiceBox<String> predicateBox;
    @FXML
    private ChoiceBox<String> operatorBox;
    @FXML
    private TextArea valueField;
    private DownloadTLE tleList;
    private EarthController earthController;
    private SatelliteController satelliteController;
    int first = 0;

    public void loadFXML(Stage mainStage) {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/ucv/run/MainView.fxml"));
            BorderPane mainBorderPanel = fxmlLoader.load();
            Scene scene = new Scene(mainBorderPanel, 1446, 779);
            mainStage.setTitle("Satellite");

            tableViewPane = (BorderPane) mainBorderPanel.lookup("#tableViewPane");
            mainStage.setScene(scene);
            mainStage.show();

        } catch (Exception ex) {
            System.out.println("An exception occurred due to load the main stage");
            ex.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tleList = new DownloadTLE();
        ObservableList<String> predicateList = FXCollections.observableArrayList();
        ObservableList<String> operatorList = FXCollections.observableArrayList();
        predicateList.add("MIN_RNG");
        predicateList.add("SAT_1_NAME");
        predicateBox.setItems(predicateList);

        operatorList.add("=");
        operatorList.add("<");
        operatorList.add(">");
        operatorList.add("≠");
        operatorBox.setItems(operatorList);
        progressBar.setProgress(-1.0);
        progressBar.setVisible(false);

        predicateBox.setValue("MIN_RNG");
        operatorBox.setValue("<");
        valueField.setText("10");
        loadEarth();
        loadTableSatellite();
        closeSession();
        mainPanel.setStyle("-fx-background-color: #ffffff; " + // culoarea fundalului
                "-fx-border-color: #4d4d4d; " + // culoarea bordurii
                "-fx-border-width: 2; " + // grosimea bordurii
                "-fx-border-radius: 5; " + // raza colțurilor
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0, 0, 0);"); // umbra

    }

    public void displaySatellites() {
        Set<DisplaySatelliteModel> satellites = satelliteController.getStringDisplaySatelliteModelMap();

        if (satellites == null || satellites.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No satellite data available.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Map<String, Ephemeris> ephemerisMap = new HashMap<>();
        Map<String, List<AbsoluteDate[]>> intervalMap = new HashMap<>();
        AbsoluteDate startDate = null;
        AbsoluteDate endDate = null;

        for (DisplaySatelliteModel model : satellites) {
            ephemerisMap.put(model.getName(), model.getEphemeris());
            intervalMap.put(model.getName(), Collections.singletonList(new AbsoluteDate[]{model.getStartDate(), model.getEndDate()}));
            if (startDate == null || model.getStartDate().compareTo(startDate) < 0) {
                startDate = model.getStartDate();
            }
            if (endDate == null || model.getEndDate().compareTo(endDate) > 0) {
                endDate = model.getEndDate();
            }
        }

        earthController.init(ephemerisMap, EarthController.wwd, earthController.getEarth(), startDate, endDate, intervalMap);
        new Thread(earthController).start();
    }


    public void loadEarth() {
        try {
            FXMLLoader fxmlLoaderEarth = new FXMLLoader(getClass().getResource("/com/ucv/run/EarthView.fxml"));
            StackPane paneWithEarth = fxmlLoaderEarth.load();
            earthController = fxmlLoaderEarth.getController();
            Stage earthStage = new Stage();
            Scene earthScene = new Scene(paneWithEarth);
            earthStage.setScene(earthScene);
            earthStage.initModality(Modality.WINDOW_MODAL);
            displayEarthButton.setOnAction(event -> {
                if (displayEarthButton.isSelected()) {
                    earthStage.show();
                    displaySatellites();
                } else {
                    earthStage.close();
                }
            });

        } catch (Exception ex) {
            System.out.println("An exception occurred due to can not instantiate the earth pane.");
        }
    }

    public void loadTableSatellite() {
        extractDataButton.setOnMouseClicked(event -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("TableSatellite.fxml"));
                BorderPane tableViewLayout = fxmlLoader.load();
                satelliteController = fxmlLoader.getController();

                String descendingTCA = "TCA%20desc";
                String operator = setOperator(operatorBox.getValue());

                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        progressBar.setVisible(true);
                        String query = String.format("/basicspacedata/query/class/cdm_public/%s/%s%s/orderby/%s/format/xml/emptyresult/show", predicateBox.getValue(), operator, valueField.getText(), descendingTCA);
                        Map<String, Item> listOfUniqueSatelliteTemp = tleList.loadSatellite(query);
                        satelliteController.setListOfUniqueSatellite(listOfUniqueSatelliteTemp);
                        Platform.runLater(() -> {
                            progressBar.setVisible(false);
                            if (tleList.getListOfUniqueSatellite().isEmpty()) {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "No results were found for the current settings", ButtonType.OK);
                                satelliteController.getSatelliteTable().refresh();
                                tableViewPane.setVisible(false);
                                alert.showAndWait();
                                event.consume();
                            } else {
                                satelliteController.getSatelliteTable().refresh();
                                successExtractList(tableViewLayout);
                            }
                        });
                        return null;
                    }
                };

                task.setOnFailed(e -> {
                    Throwable exception = task.getException();
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Error occurred: " + exception.getMessage(), ButtonType.OK);
                        alert.showAndWait();
                    });
                });
                new Thread(task).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void successExtractList(BorderPane tableViewLayout) {

        progressBar.setVisible(false);
        tableViewPane.setVisible(true);
        tableViewPane.setCenter(tableViewLayout);

    }

    private String setOperator(String operator) {
        switch (operator) {
            case "=":
                operator = "";
                break;
            case "<":
                operator = "%3C";
                break;
            case ">":
                operator = "%3E";
                break;
            default:
                operator = "%3C%3E";
                break;
        }
        return operator;
    }


    public void extractData(ActionEvent actionEvent) {
    }
}