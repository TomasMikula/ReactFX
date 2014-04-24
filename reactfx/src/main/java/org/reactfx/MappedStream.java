package org.reactfx;

import java.util.function.Function;

class MappedStream<T, U> extends LazilyBoundStream<U> {
    private final EventStream<T> input;
    private final Function<T, U> f;

    public MappedStream(EventStream<T> input, Function<T, U> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return input.subscribe(value -> {
            emit(f.apply(value));
        });
    }
}
