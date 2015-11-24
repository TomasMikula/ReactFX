package org.reactfx;

import java.util.function.Consumer;

public class PrependEventStream<T> extends EventStreamBase<T> {
    private final EventStream<T> input;
    private final T initial;

    private T latestEvent = null;

    public PrependEventStream(
            EventStream<T> input,
            T initial) {
        this.input = input;
        this.initial = initial;
    }

    @Override
    protected void newObserver(Consumer<? super T> observer) {
        if (latestEvent == null) {
            latestEvent = initial;
        }
        observer.accept(latestEvent);
    }

    @Override
    protected final Subscription observeInputs() {
        return input.subscribe(x -> {
            latestEvent = x;
            emit(latestEvent);
        });
    }
}
