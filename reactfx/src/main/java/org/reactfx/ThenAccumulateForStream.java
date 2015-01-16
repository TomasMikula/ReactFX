package org.reactfx;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;

import org.reactfx.util.Timer;

class ThenAccumulateForStream<T, A> extends EventStreamBase<T> implements AwaitingEventStream<T> {
    private static enum State {
        READY, ACC_NO_EVENT, ACC_HAS_EVENT
    }

    private final EventStream<T> input;
    private final Function<? super T, ? extends A> initial;
    private final BiFunction<? super A, ? super T, ? extends A> reduction;
    private final Function<? super A, List<T>> deconstruction;
    private final Timer timer;

    private State state = State.READY;
    private A acc = null;
    private BooleanBinding pending = null;

    public ThenAccumulateForStream(
            EventStream<T> input,
            Function<? super T, ? extends A> initial,
            BiFunction<? super A, ? super T, ? extends A> reduction,
            Function<? super A, List<T>> deconstruction,
            Function<Runnable, Timer> timerFactory) {

        this.input = input;
        this.initial = initial;
        this.reduction = reduction;
        this.deconstruction = deconstruction;
        this.timer = timerFactory.apply(this::handleTimeout);
    }

    @Override
    public ObservableBooleanValue pendingProperty() {
        if(pending == null) {
            pending = new BooleanBinding() {
                @Override
                protected boolean computeValue() {
                    return state == State.ACC_HAS_EVENT;
                }
            };
        }
        return pending;
    }

    @Override
    public boolean isPending() {
        return pending != null ? pending.get() : state == State.ACC_HAS_EVENT;
    }

    @Override
    protected final Subscription observeInputs() {
        return input.subscribe(this::handleEvent);
    }

    private void handleEvent(T t) {
        switch(state) {
        case READY:
            timer.restart();
            setState(State.ACC_NO_EVENT);
            emit(t);
            break;
        case ACC_NO_EVENT:
            acc = initial.apply(t);
            setState(State.ACC_HAS_EVENT);
            break;
        case ACC_HAS_EVENT:
            acc = reduction.apply(acc, t);
            break;
        }
    }

    private void handleTimeout() {
        List<T> toEmit;
        switch(state) {
        case ACC_HAS_EVENT:
            toEmit = deconstruction.apply(acc);
            acc = null;
            state = State.ACC_NO_EVENT; // so that recursively emitted events start accumulating
            break;
        case ACC_NO_EVENT:
            toEmit = Collections.emptyList();
            break;
        default:
            throw new AssertionError();
        }

        for(T t: toEmit) {
            emit(t);
        }

        if(state == State.ACC_NO_EVENT) { // no recursive emission occurred
            setState(State.READY);
        } else {
            // recursive emission occurred, start the timer to schedule emission
            assert state == State.ACC_HAS_EVENT;
            timer.restart();
        }
    }

    private void setState(State state) {
        this.state = state;
        invalidatePending();
    }

    private void invalidatePending() {
        if(pending != null) {
            pending.invalidate();
        }
    }
}