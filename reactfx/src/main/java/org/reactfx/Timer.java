package org.reactfx;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

interface Timer {
    void restart();
    void stop();
}

class FxTimer implements Timer {
    private final Animation animation;
    private final Runnable action;

    private long seq = 0;

    FxTimer(java.time.Duration timeout, Runnable action) {
        Duration fxTimeout = Duration.millis(timeout.toMillis());
        this.animation = new Timeline(new KeyFrame(fxTimeout));
        this.action = action;
    }

    @Override
    public void restart() {
        stop();
        long expected = seq;
        animation.setOnFinished(ae -> {
            if(seq == expected) {
                action.run();
            }
        });
        animation.play();
    }

    @Override
    public void stop() {
        animation.stop();
        ++seq;
    }
}

class ScheduledExecutorServiceTimer implements Timer {
    private final long timeout;
    private final TimeUnit unit;
    private final Runnable action;
    private final ScheduledExecutorService scheduler;
    private final Executor eventThreadExecutor;

    private ScheduledFuture<?> pendingTimer = null;
    private long seq = 0;

    public ScheduledExecutorServiceTimer(
            java.time.Duration timeout,
            Runnable action,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        this.timeout = timeout.toNanos();
        this.unit = TimeUnit.NANOSECONDS;
        this.action = action;
        this.scheduler = scheduler;
        this.eventThreadExecutor = eventThreadExecutor;
    }

    @Override
    public final void restart() {
        stop();
        long expected = seq;
        pendingTimer = scheduler.schedule(
                () -> eventThreadExecutor.execute(() -> {
                    if(seq == expected) {
                        action.run();
                    }
                }),
                timeout, unit);
    }

    @Override
    public final void stop() {
        if(pendingTimer != null) {
            pendingTimer.cancel(false);
        }
        ++seq;
    }
}