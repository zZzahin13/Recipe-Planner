package com.recipeplanner.ui;

import com.recipeplanner.model.CookingTimer;
import com.recipeplanner.network.TimerManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Lists every active CookingTimer as its own row (label + live countdown +
 * cancel button), plus a small form to start a new one. Multiple timers
 * run concurrently via the shared TimerManager -- e.g. "Pasta water" and
 * "Oven bake" both ticking down at once.
 */
public class TimerPanelView extends VBox {

    private final TimerManager timerManager;
    private final VBox timerRows = new VBox(6);
    private final TextField labelField = new TextField();
    private final Spinner<Integer> minutesSpinner = new Spinner<>(1, 240, 10);

    public TimerPanelView(TimerManager timerManager) {
        super(10);
        this.timerManager = timerManager;
        setPadding(new Insets(10));

        labelField.setPromptText("Timer name (e.g. \"Pasta water\")");
        labelField.setPrefWidth(180);
        minutesSpinner.setEditable(true);
        minutesSpinner.setPrefWidth(80);

        Button startBtn = new Button("Start New Timer");
        startBtn.getStyleClass().add("primary-button");
        startBtn.setOnAction(e -> startTimer());

        HBox form = new HBox(8, labelField, new Label("minutes:"), minutesSpinner, startBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        for (CookingTimer timer : timerManager.getTimers()) {
            timerRows.getChildren().add(buildRow(timer));
        }
        timerManager.getTimers().addListener((javafx.collections.ListChangeListener<CookingTimer>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    rebuildRows();
                }
            }
        });

        getChildren().addAll(new Label("Active Timers"), timerRows, form);
    }

    private void rebuildRows() {
        timerRows.getChildren().clear();
        for (CookingTimer timer : timerManager.getTimers()) {
            timerRows.getChildren().add(buildRow(timer));
        }
    }

    private HBox buildRow(CookingTimer timer) {
        Label nameLabel = new Label(timer.getLabel());
        nameLabel.setPrefWidth(160);

        Label countdownLabel = new Label(timer.formattedTime());
        countdownLabel.getStyleClass().add("card-title");
        timer.secondsRemainingProperty().addListener(
                (obs, oldVal, newVal) -> countdownLabel.setText(timer.formattedTime()));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("danger-button");
        cancelBtn.setOnAction(e -> timerManager.cancelTimer(timer));

        HBox row = new HBox(10, nameLabel, countdownLabel, cancelBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void startTimer() {
        String label = labelField.getText() == null || labelField.getText().isBlank()
                ? "Timer"
                : labelField.getText().trim();
        int seconds = minutesSpinner.getValue() * 60;
        timerManager.addTimer(label, seconds);
        labelField.clear();
    }
}
