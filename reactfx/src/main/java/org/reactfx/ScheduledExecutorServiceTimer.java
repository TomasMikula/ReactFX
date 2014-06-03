package org.reactfx;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.reactfx.util.Timer;

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