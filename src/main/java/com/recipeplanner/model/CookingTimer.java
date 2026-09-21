package com.recipeplanner.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * One independent countdown timer (e.g. "Pasta water", "Oven bake").
 * secondsRemaining is a JavaFX property so a ListView cell / label can
 * bind to it directly; TimerManager is the only thing that mutates it,
 * and always does so via Platform.runLater.
 */
public class CookingTimer {

    private final StringProperty label = new SimpleStringProperty();
    private final IntegerProperty totalSeconds = new SimpleIntegerProperty();
    private final IntegerProperty secondsRemaining = new SimpleIntegerProperty();
    private volatile boolean finished = false;

    public CookingTimer(String label, int totalSeconds) {
        this.label.set(label);
        this.totalSeconds.set(totalSeconds);
        this.secondsRemaining.set(totalSeconds);
    }

    public StringProperty labelProperty() {
        return label;
    }

    public String getLabel() {
        return label.get();
    }

    public IntegerProperty secondsRemainingProperty() {
        return secondsRemaining;
    }

    public int getSecondsRemaining() {
        return secondsRemaining.get();
    }

    public int getTotalSeconds() {
        return totalSeconds.get();
    }

    public boolean isFinished() {
        return finished;
    }

    public void markFinished() {
        this.finished = true;
    }

    public String formattedTime() {
        int s = getSecondsRemaining();
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
