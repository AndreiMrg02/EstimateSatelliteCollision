package com.ucv.controller;

import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.xml.Item;
import com.ucv.run.DownloadTLE;
import com.ucv.run.Main;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
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
import javafx.stage.Stage;
import javafx.util.Duration;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static com.ucv.Util.HibernateUtil.closeSession;

public class MainController implements Initializable {

    public Button stopSimulationButton;
    @FXML
    private Button closeApproachButton;
    @FXML
    private Button pauseButton;
    @FXML
    private Button showSatelliteButton;
    @FXML
    private TextArea pcTextArea;
    @FXML
    private TextArea closeApproachDistanceTextArea;
    @FXML
    private TextArea closeApproachDateTextArea;
    @FXML
    private TextArea thresholdTextArea;
    @FXML
    private TextArea startDateTextArea;
    @FXML
    private TextArea endDateTextArea;
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
    private SatelliteExtendController satelliteController;
    private AbsoluteDate closeApproachDate = new AbsoluteDate();
    Stage earthStage = new Stage();
    int first = 0;

    public void loadFXML(Stage mainStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/ucv/run/NewIdeea/MainView.fxml"));
            BorderPane mainBorderPanel = fxmlLoader.load();
            Scene scene = new Scene(mainBorderPanel, 1400, 900);
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
        operatorBox.setItems(operatorList);
        progressBar.setProgress(-1.0);
        progressBar.setVisible(false);

        predicateBox.setValue("MIN_RNG");
        operatorBox.setValue("<");
        valueField.setText("10");
        setButtonStyle(showSatelliteButton);
        setButtonStyle(pauseButton);
        setButtonStyle(closeApproachButton);

        loadEarth();
        loadTableSatellite();
        closeSession();

        showSatelliteButton.setOnAction(event -> {
            displaySatellites();
            pauseButton.setDisable(false);
            stopSimulationButton.setDisable(false);
            closeApproachButton.setDisable(false);
            showSatelliteButton.setDisable(true);
        });

        stopSimulationButton.setOnAction(event -> {
            earthController.delete();
            showSatelliteButton.setDisable(false);
            pauseButton.setDisable(true);
            closeApproachButton.setDisable(true);
            stopSimulationButton.setDisable(true);
        });

        pauseButton.setOnAction(event -> {
            earthController.pauseSimulation();
            pauseButton.setDisable(true);
        });

        closeApproachButton.setOnAction(event -> {
            showSatellitesAtCloseApproach();
        });
    }

    public void displaySatellites() {
        List<DisplaySatelliteModel> satellites = satelliteController.getTwoSatellitesSelected();

        if (satellites == null || satellites.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No satellite data available.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        earthController.resetState(); // Asigură-te că resetezi starea înainte de a inițializa o nouă simulare

        Map<String, Ephemeris> ephemerisMap = new HashMap<>();
        Map<String, List<AbsoluteDate[]>> intervalMap = new HashMap<>();
        AbsoluteDate startDate = null;
        AbsoluteDate endDate = null;
        AbsoluteDate closeApproach = null;
        for (DisplaySatelliteModel model : satellites) {
            ephemerisMap.put(model.getName(), model.getEphemeris());
            intervalMap.put(model.getName(), Collections.singletonList(new AbsoluteDate[]{model.getStartDate(), model.getEndDate()}));
            if (startDate == null || model.getStartDate().compareTo(startDate) < 0) {
                startDate = model.getStartDate();
            }
            if (endDate == null || model.getEndDate().compareTo(endDate) > 0) {
                endDate = model.getEndDate();
            }
            closeApproach = model.getCloseApproachDate();
        }

        earthController.init(ephemerisMap, EarthController.wwd, earthController.getEarth(), startDate, endDate, intervalMap, closeApproach);
        earthController.startSimulation();
    }

    public void showSatellitesAtCloseApproach() {
        earthController.pauseSimulation(); // Pauză temporară pentru a actualiza poziția sferei la data apropierii maxime
        AbsoluteDate closeApproachDate = earthController.getCloseApproachDate();
        earthController.updateSatellites(closeApproachDate); // Actualizează sfera la data apropierii maxime
        EarthController.wwd.redraw(); // Redesenare pentru a reflecta schimbările
        earthController.setStartDate(closeApproachDate); // Setează data de început la data apropierii maxime
        earthController.resumeSimulation(); // Reia simularea de la data apropierii maxime
        pauseButton.setDisable(false);
    }



    public void loadEarth() {
        try {
            FXMLLoader fxmlLoaderEarth = new FXMLLoader(getClass().getResource("/com/ucv/run/EarthView.fxml"));
            StackPane paneWithEarth = fxmlLoaderEarth.load();
            earthController = fxmlLoaderEarth.getController();
            //  earthPane = (StackPane) paneWithEarth.lookup("#earthPane");
            earthPane.getChildren().add(paneWithEarth);

            //    earthStage.setOnHiding(event -> earthController.clearSpheres());
        } catch (Exception ex) {
            System.out.println("An exception occurred due to can not instantiate the earth pane.");
            ex.printStackTrace();
        }
    }

    public void loadTableSatellite() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("TableSatelliteExtended.fxml"));
            BorderPane tableViewLayout = fxmlLoader.load();
            satelliteController = fxmlLoader.getController();
            successExtractList(tableViewLayout);
        } catch (IOException e) {
            e.printStackTrace();
        }
        extractDataButton.setOnMouseClicked(event -> {
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
                            setTextArea();
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
        });
    }

    private void successExtractList(BorderPane tableViewLayout) {
        progressBar.setVisible(false);
        tableViewPane.setVisible(true);
        tableViewPane.setCenter(tableViewLayout);
    }

    private void setButtonStyle(Button button) {
        button.setOnMouseEntered(e -> {
            button.getStyleClass().add("buttonBackgroundHovered");
            ScaleTransition st = new ScaleTransition(Duration.millis(500), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), button);
            tt.setToY(-10);
            tt.play();
        });

        button.setOnMouseExited(e -> {
            button.getStyleClass().remove("buttonBackgroundHovered");
            ScaleTransition st = new ScaleTransition(Duration.millis(500), button);
            st.setToX(1);
            st.setToY(1);
            st.play();
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), button);
            tt.setToY(0);
            tt.play();
        });
    }

    private void setTextArea() {
        satelliteController.getSatelliteTable().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showSatelliteButton.setDisable(false);
                pcTextArea.setText(newValue.getCollisionProbability());
                startDateTextArea.setText(newValue.getStartDate());
                endDateTextArea.setText(newValue.getEndDate());
                closeApproachDateTextArea.setText(newValue.getCloseApproachDate());
                closeApproachDistanceTextArea.setText(newValue.getCloseApproach());
                thresholdTextArea.setText("1000");
            }
        });
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
