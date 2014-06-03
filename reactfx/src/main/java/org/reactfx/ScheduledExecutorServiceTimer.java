package org.reactfx;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.reactfx.util.Timer;
import org.reactfx.util.TriFunction;

class ScheduledExecutorServiceTimer implements Timer {

    public static Timer create(
            java.time.Duration timeout,
            Runnable action,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {
        return new ScheduledExecutorServiceTimer(
                timeout, action,
                (delay, unit, cmd) -> scheduler.schedule(cmd, delay, unit),
                eventThreadExecutor);
    }

    public static Timer createPeriodic(
            java.time.Duration timeout,
            Runnable action,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {
        return new ScheduledExecutorServiceTimer(
                timeout, action,
                (delay, unit, cmd) -> scheduler.scheduleAtFixedRate(cmd, delay, delay, unit),
                eventThreadExecutor);
    }

    private final long timeout;
    private final TimeUnit unit;
    private final Runnable action;
    private final TriFunction<Long, TimeUnit, Runnable, ScheduledFuture<?>> scheduler;
    private final Executor eventThreadExecutor;

    private ScheduledFuture<?> pendingTimer = null;
    private long seq = 0;

    private ScheduledExecutorServiceTimer(
            java.time.Duration timeout,
            Runnable action,
            TriFunction<Long, TimeUnit, Runnable, ScheduledFuture<?>> scheduler,
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
        pendingTimer = scheduler.apply(timeout, unit, () -> {
            eventThreadExecutor.execute(() -> {
                if(seq == expected) {
                    action.run();
                }
            });
        });
    }

    @Override
    public final void stop() {
        if(pendingTimer != null) {
            pendingTimer.cancel(false);
        }
        ++seq;
    }
}