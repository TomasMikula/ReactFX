package org.reactfx;

import java.util.function.Consumer;

import org.reactfx.util.NotificationAccumulator;


class RecursiveStream<T> extends EventStreamBase<T> {
    private final EventStream<T> input;

    public RecursiveStream(
            EventStream<T> input,
            NotificationAccumulator<Consumer<? super T>, T, ?> pn) {
        super(pn);
        this.input = input;
    }

    @Override
    protected Subscription bindToInputs() {
        return input.subscribe(this::emit);
    }
}