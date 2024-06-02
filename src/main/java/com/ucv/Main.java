package com.ucv;


import com.ucv.controller.MainController;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import java.io.File;


public class Main extends Application {
    @Override
    public void start(Stage stage) {
        BasicConfigurator.configure();
        MainController mainController = new MainController();
        mainController.loadFXML(stage);
        stage.setOnCloseRequest(windowEvent -> System.exit(0));
    }

    public static void main(String[] args) {

        File orekitData = new File("data/orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
        launch();

    }
}