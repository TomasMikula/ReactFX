package org.reactfx;

import java.util.function.Consumer;

public class PrependEventStream<T> extends EventStreamBase<T> {
    private final EventStream<T> input;
    private final T initial;

    private T latestEvent = null;
    private boolean firstObserver = true;

    public PrependEventStream(
            EventStream<T> input,
            T initial) {
        this.input = input;
        this.initial = initial;
    }

    @Override
    protected void newObserver(Consumer<? super T> observer) {
        if(firstObserver) {
            firstObserver = false;
        } else {
            observer.accept(latestEvent);
        }
    }

    @Override
    protected final Subscription observeInputs() {
        firstObserver = true;
        latestEvent = initial;
        emit(initial); // emit for the first observer
        return input.subscribe(x -> {
            latestEvent = x;
            emit(x);
        });
    }
}
