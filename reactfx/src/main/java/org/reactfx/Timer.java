package org.reactfx;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

interface Timer {
    void reset(Runnable action);
}

class TimelineTimer implements Timer {
    private final Timeline timeline;

    public TimelineTimer(java.time.Duration timeout) {
        Duration fxTimeout = Duration.millis(timeout.toMillis());
        this.timeline = new Timeline(new KeyFrame(fxTimeout));
    }

    @Override
    public void reset(Runnable action) {
        timeline.stop();
        timeline.setOnFinished(ae -> action.run());
        timeline.playFromStart();
    }
}

class ScheduledExecutorServiceTimer implements Timer {
    private final long timeout;
    private final TimeUnit unit;
    private final ScheduledExecutorService scheduler;
    private final Executor eventThreadExecutor;

    private ScheduledFuture<?> pendingTimer = null;

    public ScheduledExecutorServiceTimer(
            java.time.Duration timeout,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        this.timeout = timeout.toNanos();
        this.unit = TimeUnit.NANOSECONDS;
        this.scheduler = scheduler;
        this.eventThreadExecutor = eventThreadExecutor;
    }

    @Override
    public void reset(Runnable action) {
        if(pendingTimer != null) {
            pendingTimer.cancel(false);
        }
        pendingTimer = scheduler.schedule(() -> eventThreadExecutor.execute(action), timeout, unit);
    }
}