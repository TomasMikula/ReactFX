package org.reactfx;

import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;

import org.reactfx.util.Timer;

class SuccessionReducingStream<I, O> extends EventStreamBase<O> implements AwaitingEventStream<O> {
    private final EventStream<I> input;
    private final Function<? super I, ? extends O> initial;
    private final BiFunction<? super O, ? super I, ? extends O> reduction;
    private final Timer timer;

    private boolean hasEvent = false;
    private BooleanBinding pending = null;
    private O event = null;

    public SuccessionReducingStream(
            EventStream<I> input,
            Function<? super I, ? extends O> initial,
            BiFunction<? super O, ? super I, ? extends O> reduction,
            Function<Runnable, Timer> timerFactory) {

        this.input = input;
        this.initial = initial;
        this.reduction = reduction;
        this.timer = timerFactory.apply(this::handleTimeout);
    }

    @Override
    public ObservableBooleanValue pendingProperty() {
        if(pending == null) {
            pending = new BooleanBinding() {
                @Override
                protected boolean computeValue() {
                    return hasEvent;
                }
            };
        }
        return pending;
    }

    @Override
    public boolean isPending() {
        return pending != null ? pending.get() : hasEvent;
    }

    @Override
    protected final Subscription subscribeToInputs() {
        return subscribeTo(input, this::handleEvent);
    }

    private void handleEvent(I i) {
        if(hasEvent) {
            event = reduction.apply(event, i);
        } else {
            assert event == null;
            event = initial.apply(i);
            hasEvent = true;
            invalidatePending();
        }
        timer.restart();
    }

    private void handleTimeout() {
        assert hasEvent;
        hasEvent = false;
        O toEmit = event;
        event = null;
        emit(toEmit);
        invalidatePending();
    }

    private void invalidatePending() {
        if(pending != null) {
            pending.invalidate();
        }
    }
}