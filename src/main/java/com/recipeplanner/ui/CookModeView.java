package com.recipeplanner.ui;

import com.recipeplanner.model.Recipe;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Fullscreen, distraction-free "Cook Mode": one big instruction step at a
 * time, Space/Right-arrow to advance, Left-arrow to go back, completed
 * steps shown struck through. Opens in its own Stage rather than
 * swapping into MainView's content area, so it doesn't need the
 * primary Stage threaded through the rest of the app -- ESC (JavaFX's
 * default fullscreen exit key) closes it.
 */
public final class CookModeView {

    // Splits instructions into steps on blank lines, numbered "1." / "1)"
    // markers, or newlines -- whichever the recipe's instructions actually use.
    private static final Pattern STEP_SPLIT =
            Pattern.compile("\\r?\\n+|(?=\\b\\d{1,2}[.)]\\s)");

    private CookModeView() {
    }

    public static void open(Recipe recipe, Stage owner) {
        List<String> steps = splitIntoSteps(recipe.getInstructions());
        if (steps.isEmpty()) {
            steps = List.of("No instructions available for this recipe.");
        }

        int[] currentIndex = {0};
        boolean[] completed = new boolean[steps.size()];

        Label stepCountLabel = new Label();
        Label stepText = new Label();
        stepText.setWrapText(true);
        stepText.setFont(Font.font("System", FontWeight.BOLD, 34));
        stepText.setTextFill(Color.WHITE);
        stepText.setMaxWidth(900);

        Button prevBtn = new Button("< Back");
        Button nextBtn = new Button("Next >");
        Button doneBtn = new Button("Mark Done");
        Button exitBtn = new Button("Exit Cook Mode (Esc)");
        for (Button b : List.of(prevBtn, nextBtn, doneBtn, exitBtn)) {
            b.getStyleClass().add("primary-button");
        }

        VBox center = new VBox(24, stepCountLabel, stepText);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));

        HBox controls = new HBox(14, prevBtn, doneBtn, nextBtn);
        controls.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1B1F1B;");
        root.setCenter(center);
        root.setBottom(controls);
        BorderPane.setAlignment(controls, Pos.CENTER);
        BorderPane.setMargin(controls, new Insets(0, 0, 30, 0));

        HBox topBar = new HBox(exitBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(16));
        root.setTop(topBar);

        List<String> finalSteps = steps;
        Runnable render = () -> {
            int i = currentIndex[0];
            stepCountLabel.setText("Step " + (i + 1) + " of " + finalSteps.size()
                    + (completed[i] ? "  (done)" : ""));
            stepCountLabel.setTextFill(Color.LIGHTGRAY);
            String text = finalSteps.get(i);
            stepText.setText(completed[i] ? "\u2713  " + text : text);
            stepText.setStyle(completed[i] ? "-fx-strikethrough: true; -fx-opacity: 0.6;" : "");
            prevBtn.setDisable(i == 0);
            nextBtn.setDisable(i == finalSteps.size() - 1);
        };

        prevBtn.setOnAction(e -> {
            if (currentIndex[0] > 0) {
                currentIndex[0]--;
                render.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentIndex[0] < finalSteps.size() - 1) {
                currentIndex[0]++;
                render.run();
            }
        });
        doneBtn.setOnAction(e -> {
            completed[currentIndex[0]] = true;
            if (currentIndex[0] < finalSteps.size() - 1) {
                currentIndex[0]++;
            }
            render.run();
        });

        Stage stage = new Stage(StageStyle.UNDECORATED);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Cook Mode -- " + recipe.getTitle());

        Scene scene = new Scene(root, 1000, 650, Color.BLACK);
        exitBtn.setOnAction(e -> stage.close());
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.SPACE || code == KeyCode.RIGHT) {
                nextBtn.fire();
            } else if (code == KeyCode.LEFT) {
                prevBtn.fire();
            } else if (code == KeyCode.ESCAPE) {
                stage.close();
            } else if (code == KeyCode.ENTER) {
                doneBtn.fire();
            }
        });

        stage.setScene(scene);
        stage.setFullScreenExitHint("Press ESC or click Exit Cook Mode to leave");
        stage.setFullScreen(true);
        render.run();
        stage.show();
        root.requestFocus();
    }

    private static List<String> splitIntoSteps(String instructions) {
        List<String> steps = new ArrayList<>();
        if (instructions == null || instructions.isBlank()) {
            return steps;
        }
        for (String piece : STEP_SPLIT.split(instructions)) {
            String cleaned = piece.strip();
            // Strip a leading "1." / "2)" numbering marker since the UI shows its own step count.
            cleaned = cleaned.replaceFirst("^\\d{1,2}[.)]\\s*", "");
            if (!cleaned.isEmpty()) {
                steps.add(cleaned);
            }
        }
        return steps;
    }
}
