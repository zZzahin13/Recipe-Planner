package com.recipeplanner;

import com.recipeplanner.db.DatabaseManager;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Application entry point. Both the splash screen and the main app
 * shell are now FXML (see /fxml/splash.fxml, /fxml/main.fxml, and
 * MainController), loaded here via FXMLLoader rather than being built
 * with Java code -- open either file in Scene Builder to edit the layout.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Touch the DB layer on startup so schema creation happens once,
        // before any screen tries to query it.
        DatabaseManager.getConnection();

        showSplash(primaryStage);
    }

    private void showSplash(Stage primaryStage) throws IOException {
        Parent splashRoot = FXMLLoader.load(getClass().getResource("/fxml/splash.fxml"));

        Stage splashStage = new Stage(StageStyle.UNDECORATED);
        splashStage.setScene(new Scene(splashRoot));
        splashStage.centerOnScreen();
        splashStage.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(1.8));
        pause.setOnFinished(e -> {
            splashStage.close();
            try {
                showMainWindow(primaryStage);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not load main.fxml", ex);
            }
        });
        pause.play();
    }

    private void showMainWindow(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 720);
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
