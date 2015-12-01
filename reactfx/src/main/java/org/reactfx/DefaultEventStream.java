package org.reactfx;

import java.util.function.Consumer;

class DefaultEventStream<T> extends EventStreamBase<T> {
    private final EventStream<T> input;
    private final T initial;

    private T latestEvent = null;
    private boolean firstObserver = true;
    private boolean emitted = false;

    public DefaultEventStream(
            EventStream<T> input,
            T initial) {
        this.input = input;
        this.initial = initial;
    }

    @Override
    protected void newObserver(Consumer<? super T> observer) {
        if(firstObserver) {
            firstObserver = false;
            if(!emitted) {
                observer.accept(initial);
            }
        } else {
            observer.accept(latestEvent);
        }
    }

    @Override
    protected final Subscription observeInputs() {
        firstObserver = true;
        emitted = false;
        latestEvent = initial;
        return input.subscribe(x -> {
            latestEvent = x;
            emitted = true;
            emit(x);
        });
    }
}
