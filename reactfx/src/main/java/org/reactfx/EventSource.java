package org.reactfx;

import java.util.function.Consumer;

/**
 * EventSource is a Sink that serves also as an EventStream - every value
 * pushed to EventSource is immediately emitted by it.
 * @param <T> type of values this EventSource accepts and emits.
 */
public class EventSource<T> extends EventStreamBase<Consumer<? super T>> implements EventStream<T>, Sink<T> {

    /**
     * Make this event stream immediately emit the given value.
     */
    @Override
    public void push(T value) {
        forEachSubscriber(s -> s.accept(value));
    }
}
