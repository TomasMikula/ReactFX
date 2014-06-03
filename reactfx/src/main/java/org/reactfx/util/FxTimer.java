package org.reactfx.util;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Provides factory methods for timers that are manipulated from and execute
 * their action on the JavaFX application thread.
 */
public class FxTimer implements Timer {

    /**
     * Prepares a (stopped) timer with the given delay and action.
     */
    public static Timer create(java.time.Duration delay, Runnable action) {
        return new FxTimer(delay, action, 1);
    }

    /**
     * Equivalent to {@code create(delay, action).restart()}.
     */
    public static Timer runLater(java.time.Duration delay, Runnable action) {
        Timer timer = create(delay, action);
        timer.restart();
        return timer;
    }

    /**
     * Prepares a (stopped) timer that executes the given action periodically
     * with the given interval.
     */
    public static Timer createPeriodic(java.time.Duration interval, Runnable action) {
        return new FxTimer(interval, action, Animation.INDEFINITE);
    }

    /**
     * Equivalent to {@code createPeriodic(interval, action).restart()}.
     */
    public static Timer runPeriodically(java.time.Duration interval, Runnable action) {
        Timer timer = createPeriodic(interval, action);
        timer.restart();
        return timer;
    }

    private final Duration timeout;
    private final Timeline timeline;
    private final Runnable action;

    private long seq = 0;

    private FxTimer(java.time.Duration timeout, Runnable action, int cycles) {
        this.timeout = Duration.millis(timeout.toMillis());
        this.timeline = new Timeline();
        this.action = action;

        timeline.setCycleCount(cycles);
    }

    @Override
    public void restart() {
        stop();
        long expected = seq;
        timeline.getKeyFrames().setAll(new KeyFrame(timeout, ae -> {
            if(seq == expected) {
                action.run();
            }
        }));
        timeline.play();
    }

    @Override
    public void stop() {
        timeline.stop();
        ++seq;
    }
}