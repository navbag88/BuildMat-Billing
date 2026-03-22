package com.buildmat;

import com.buildmat.ui.LoginScreen;
import com.buildmat.util.DatabaseManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) {
        log.info("BuildMat Billing application starting");
        log.info("Log file: {}", System.getProperty("log.path", "logs/buildmat.log"));
        try {
            DatabaseManager.initializeDatabase();
            log.info("Database initialized successfully");
            LoginScreen loginScreen = new LoginScreen();
            loginScreen.show(primaryStage);
            log.info("Login screen displayed");
        } catch (Exception e) {
            log.error("Fatal error during application startup", e);
            throw e;
        }
    }

    @Override
    public void stop() {
        log.info("BuildMat Billing application shutting down");
    }

    public static void main(String[] args) {
        log.info("Launching application, Java version: {}", System.getProperty("java.version"));
        launch(args);
    }
}
