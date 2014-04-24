package org.reactfx;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javafx.concurrent.Task;

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

class MappedToCompletionStageStream<T, U>
extends MappedStream<T, CompletionStage<U>>
implements CompletionStageStream<U> {

    public MappedToCompletionStageStream(
            EventStream<T> input,
            Function<T, CompletionStage<U>> f) {
        super(input, f);
    }
}

class MappedToTaskStream<T, U>
extends MappedStream<T, Task<U>>
implements TaskStream<U> {

    public MappedToTaskStream(EventStream<T> input, Function<T, Task<U>> f) {
        super(input, f);
    }
}