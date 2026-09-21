package com.recipeplanner;

import com.recipeplanner.db.DatabaseManager;
import com.recipeplanner.ui.MainView;
import com.recipeplanner.ui.SplashScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Touch the DB layer on startup so schema creation happens once,
        // before any screen tries to query it.
        DatabaseManager.getConnection();

        SplashScreen.show(() -> showMainWindow(primaryStage));
    }

    private void showMainWindow(Stage primaryStage) {
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("Recipe Planner");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        DatabaseManager.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

