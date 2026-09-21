package com.recipeplanner.network;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 * Background countdown timer for the "Cooking Timer" feature. Runs on a
 * daemon background thread owned by javafx.concurrent.Service, so it
 * keeps ticking even if the user navigates away from the recipe detail
 * screen; secondsRemaining is only ever mutated via Platform.runLater
 * to stay safe for the JavaFX Application Thread.
 */
public class CookingTimerService extends Service<Void> {

    private final IntegerProperty secondsRemaining = new SimpleIntegerProperty(0);
    private final BooleanProperty finished = new SimpleBooleanProperty(false);

    public void start(int totalSeconds) {
        if (getState() == State.RUNNING) {
            cancel();
        }
        secondsRemaining.set(totalSeconds);
        finished.set(false);
        restart();
    }

    public IntegerProperty secondsRemainingProperty() {
        return secondsRemaining;
    }

    public BooleanProperty finishedProperty() {
        return finished;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                while (secondsRemaining.get() > 0 && !isCancelled()) {
                    Thread.sleep(1000);
                    Platform.runLater(() -> secondsRemaining.set(secondsRemaining.get() - 1));
                }
                if (!isCancelled()) {
                    Platform.runLater(() -> finished.set(true));
                }
                return null;
            }
        };
    }
}
