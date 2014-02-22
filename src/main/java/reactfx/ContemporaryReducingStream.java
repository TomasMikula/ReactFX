package reactfx;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

abstract class ContemporaryReducingStream<I, O> extends LazilyBoundStream<O> {
    private final EventStream<I> input;
    private final Function<I, O> initial;
    private final BiFunction<O, I, O> reduction;

    private long timerNumber = 0;
    private boolean hasEvent = false;
    private O event = null;

    public ContemporaryReducingStream(
            EventStream<I> input,
            Function<I, O> initial,
            BiFunction<O, I, O> reduction) {

        this.input = input;
        this.initial = initial;
        this.reduction = reduction;
    }

    @Override
    protected final Subscription subscribeToInputs() {
        return input.subscribe(i -> handleEvent(i));
    }

    protected abstract void resetTimer(Runnable action);

    private void handleEvent(I i) {
        if(hasEvent) {
            event = reduction.apply(event, i);
        } else {
            event = initial.apply(i);
            hasEvent = true;
        }
        resetTimer();
    }

    private void resetTimer() {
        long nextTimer = ++timerNumber;
        resetTimer(() -> {
            if(nextTimer == timerNumber) {
                handleTimeout();
            }
        });
    }

    private void handleTimeout() {
        emit(event);
        event = null;
        hasEvent = false;
    }
}


class FxContemporaryReducingStream<I, O> extends ContemporaryReducingStream<I, O> {
    private final Timeline timeline;

    public FxContemporaryReducingStream(
            EventStream<I> input,
            Function<I, O> initial,
            BiFunction<O, I, O> reduction,
            Duration timeout) {

        super(input, initial, reduction);
        this.timeline = new Timeline(new KeyFrame(timeout));
    }

    @Override
    protected final void resetTimer(Runnable action) {
        timeline.stop();
        timeline.setOnFinished(ae -> action.run());
        timeline.playFromStart();
    }
}

class SchedulerContemporaryReducingStream<I, O> extends ContemporaryReducingStream<I, O> {
    private final long timeout;
    private final TimeUnit unit;
    private final ScheduledExecutorService scheduler;
    private final Executor eventThreadExecutor;

    private ScheduledFuture<?> pendingTimer = null;

    public SchedulerContemporaryReducingStream(
            EventStream<I> input,
            Function<I, O> initial,
            BiFunction<O, I, O> reduction,
            long timeout, TimeUnit unit,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        super(input, initial, reduction);
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.eventThreadExecutor = eventThreadExecutor;
    }

    @Override
    protected final void resetTimer(Runnable action) {
        if(pendingTimer != null) {
            pendingTimer.cancel(false);
        }
        pendingTimer = scheduler.schedule(() -> eventThreadExecutor.execute(action), timeout, unit);
    }
}