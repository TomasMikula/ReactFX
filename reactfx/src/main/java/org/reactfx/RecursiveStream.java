package org.reactfx;

import org.reactfx.util.NotificationAccumulator;


class RecursiveStream<T> extends EventStreamBase<T> {
    private final EventStream<T> input;

    public RecursiveStream(
            EventStream<T> input,
            NotificationAccumulator<Subscriber<? super T>, T> pn) {
        super(pn);
        this.input = input;
    }

    @Override
    protected Subscription bindToInputs() {
        return subscribeTo(input, this::emit);
    }
}