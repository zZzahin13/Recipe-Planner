package com.recipeplanner.network;

import com.recipeplanner.model.CookingTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.awt.Toolkit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs any number of independent named countdown timers concurrently
 * (Week 4: ScheduledExecutorService + thread-safe shared state) so
 * several simmering/baking steps can tick down at once while the user
 * browses other screens. One shared background thread ticks every
 * second; all UI-visible property changes are marshalled back to the
 * JavaFX Application Thread via Platform.runLater.
 *
 * Sound notification uses the system beep (java.awt.Toolkit) rather
 * than an embedded audio file/MediaPlayer, since that needs no bundled
 * asset and works the same on every platform without extra setup.
 */
public class TimerManager {

    private final ObservableList<CookingTimer> timers = FXCollections.observableArrayList();
    // CopyOnWriteArrayList because the scheduler thread iterates this list
    // every second while the JavaFX thread may add/remove timers concurrently.
    private final List<CookingTimer> activeTimers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cooking-timer-ticker");
        t.setDaemon(true);
        return t;
    });

    private Consumer<CookingTimer> onTimerFinished = timer -> {
    };

    public TimerManager() {
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    /** Called (on the JavaFX thread) whenever a timer hits zero -- wire this to show an Alert. */
    public void setOnTimerFinished(Consumer<CookingTimer> callback) {
        this.onTimerFinished = callback != null ? callback : t -> {
        };
    }

    public ObservableList<CookingTimer> getTimers() {
        return timers;
    }

    public CookingTimer addTimer(String label, int totalSeconds) {
        CookingTimer timer = new CookingTimer(label, totalSeconds);
        Platform.runLater(() -> timers.add(timer));
        activeTimers.add(timer);
        return timer;
    }

    public void cancelTimer(CookingTimer timer) {
        activeTimers.remove(timer);
        Platform.runLater(() -> timers.remove(timer));
    }

    /** Runs on the scheduler's background thread every second. */
    private void tick() {
        for (CookingTimer timer : activeTimers) {
            int remaining = timer.getSecondsRemaining();
            if (remaining <= 0) {
                continue;
            }
            int updated = remaining - 1;
            Platform.runLater(() -> timer.secondsRemainingProperty().set(updated));

            if (updated == 0) {
                activeTimers.remove(timer);
                timer.markFinished();
                Toolkit.getDefaultToolkit().beep();
                Platform.runLater(() -> onTimerFinished.accept(timer));
            }
        }
    }

    /** Call on application shutdown to stop the background ticker thread cleanly. */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
