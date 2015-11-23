package org.reactfx;

public class PrependEventStream<T> extends EventStreamBase<T> {
    private final EventStream<T> input;
    private final T initial;

    private boolean hasEvent = false;
    private T event = null;

    public PrependEventStream(
            EventStream<T> input,
            T initial) {
        this.input = input;
        this.initial = initial;
    }

    @Override
    protected final Subscription observeInputs() {
        return input.subscribe(e -> {
            event = hasEvent
                    ? e
                    : initial;
            hasEvent = true;
            emit(event);
        });
    }
}
