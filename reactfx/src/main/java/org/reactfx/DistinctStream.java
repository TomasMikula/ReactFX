package org.reactfx;

import java.util.Objects;


class DistinctStream<T> extends EventStreamBase<T> {
    static final Object NONE = new Object();
    private final EventStream<T> input;
    private Object previous = NONE;

    public DistinctStream(EventStream<T> input) {
        this.input = input;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(input, value -> {
            Object prevToCompare = previous;
            previous = value;
            if (!Objects.equals(value, prevToCompare)) {
                emit(value);
            }
        });
    }
}