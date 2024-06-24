package com.ucv;


import com.ucv.controller.LoginController;
import com.ucv.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import java.io.File;

import static com.ucv.controller.EarthViewController.wwd;


public class Main extends Application {
    private Scene mainScene;
    private final Logger logger = LogManager.getLogger(Main.class);

    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            BorderPane mainBorderPanel = fxmlLoader.load();
            MainController mainController = fxmlLoader.getController();
            mainScene = new Scene(mainBorderPanel);
            stage.setTitle("Satellite");
            stage.setScene(mainScene);
            loadLoginScreen(stage, mainController);
            BasicConfigurator.configure();
            stage.setOnCloseRequest(windowEvent -> System.exit(0));
        } catch (Exception ex) {
            logger.error("Unexpected error due to start application");
        }
    }

    public static void main(String[] args) {
        File orekitData = new File("data/orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
        launch();
    }

    private void loadLoginScreen(Stage primaryStage, MainController mainController) {
        try {
            FXMLLoader fxmlLoaderInformation = new FXMLLoader(Main.class.getResource("/views/login.fxml"));
            VBox root = fxmlLoaderInformation.load();
            LoginController loginController = fxmlLoaderInformation.getController();
            Scene loginScene = new Scene(root);
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(loginScene);
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.showAndWait();
            mainController.getMinimizeButton().setOnMouseClicked(mouseEvent -> primaryStage.setIconified(true));
            if (loginController.isAuthenticated()) {
                Platform.runLater(() -> {
                    mainController.setConnectionData(loginController.getInternetConnectionData());
                    try {
                        mainController.getCloseButton().setOnMouseClicked((MouseEvent mouseEvent) -> {
                            wwd.shutdown();
                            System.exit(0);
                        });
                    } catch (Exception e) {
                        logger.error("An error occurred while setting the close button action", e);
                    }
                    primaryStage.setScene(mainScene);
                    primaryStage.initStyle(StageStyle.UNDECORATED);
                    primaryStage.show();
                });
            } else {
                loginStage.showAndWait();
                logger.error("Authentication failed.");
            }
        } catch (Exception ex) {
            logger.error("Unexpected error due to login process.");

        }
    }
}
