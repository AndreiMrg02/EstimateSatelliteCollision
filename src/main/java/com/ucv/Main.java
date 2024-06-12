package com.ucv;


import com.ucv.controller.LoginController;
import com.ucv.controller.MainController;
import com.ucv.util.PaneCustomStyle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.log4j.BasicConfigurator;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import java.io.File;


public class Main extends Application {
    private Scene mainScene;

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
            stage.setOnCloseRequest(windowEvent -> Platform.runLater(() -> System.exit(0)));
        } catch (Exception ex) {
            ex.printStackTrace();
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
            PaneCustomStyle customStyle = new PaneCustomStyle();
            FXMLLoader fxmlLoaderInformation = new FXMLLoader(Main.class.getResource("/views/login.fxml"));
            VBox root = fxmlLoaderInformation.load();
            LoginController loginController = fxmlLoaderInformation.getController();
            Scene loginScene = new Scene(root);
            Stage loginStage = new Stage();
            customStyle.addClip(root, 15, 15);
            loginStage.setTitle("Login");
            loginStage.setScene(loginScene);
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.showAndWait();
            // Show login stage and wait for it to close
            if (loginController.isAuthenticated()) {
                Platform.runLater(() -> {
                    mainController.setConnectionData(loginController.getInternetConnectionData());
                    primaryStage.setScene(mainScene);
                    primaryStage.show();
                });
            } else {
                loginStage.showAndWait();
                System.out.println("Authentication failed.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
