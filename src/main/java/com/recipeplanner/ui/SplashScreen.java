package com.recipeplanner.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Brief welcome screen shown on launch: project name + tagline on a
 * branded background, auto-closing into the real app after a short
 * pause. Runs as its own small undecorated Stage rather than a scene
 * inside the main window, so it can appear before the (heavier) main
 * UI has finished building.
 */
public final class SplashScreen {

    private SplashScreen() {
    }

    /** Shows the splash, then calls onFinished (and closes the splash) after a short delay. */
    public static void show(Runnable onFinished) {
        Label title = new Label("Recipe Planner");
        title.setFont(Font.font("System", FontWeight.BOLD, 40));
        title.setTextFill(Color.WHITE);

        Label tagline = new Label("Search, save, and plan your meals in one place");
        tagline.setFont(Font.font("System", 15));
        tagline.setTextFill(Color.web("#D8E6D8"));

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(36, 36);
        spinner.setStyle("-fx-progress-color: white;");

        VBox root = new VBox(16, title, tagline, spinner);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E3B2E, #465946);");

        Stage splashStage = new Stage(StageStyle.UNDECORATED);
        splashStage.setScene(new Scene(root, 480, 320));
        splashStage.centerOnScreen();
        splashStage.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(1.8));
        pause.setOnFinished(e -> {
            splashStage.close();
            if (onFinished != null) {
                onFinished.run();
            }
        });
        pause.play();
    }
}
