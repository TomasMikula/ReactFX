package org.reactfx;

import java.util.function.BiFunction;
import java.util.function.Function;

class SuccessionReducingStream<I, O> extends LazilyBoundStream<O> {
    private final EventStream<I> input;
    private final Function<? super I, ? extends O> initial;
    private final BiFunction<? super O, ? super I, ? extends O> reduction;
    private final Timer timer;

    private long timerNumber = 0;
    private boolean hasEvent = false;
    private O event = null;

    public SuccessionReducingStream(
            EventStream<I> input,
            Function<? super I, ? extends O> initial,
            BiFunction<? super O, ? super I, ? extends O> reduction,
            Timer timer) {

        this.input = input;
        this.initial = initial;
        this.reduction = reduction;
        this.timer = timer;
    }

    @Override
    protected final Subscription subscribeToInputs() {
        return input.subscribe(i -> handleEvent(i));
    }

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
        timer.reset(() -> {
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