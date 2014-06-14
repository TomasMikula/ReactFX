package org.reactfx;

import java.util.function.BiFunction;
import java.util.function.Function;

class AccumulatingStream<T, U> extends LazilyBoundStream<U> {
    private final EventStream<T> input;
    private final Function<? super T, ? extends U> initialTransformation;
    private final BiFunction<? super U, ? super T, ? extends U> reduction;

    private boolean hasEvent = false;
    private U event = null;

    public AccumulatingStream(
            EventStream<T> input,
            Function<? super T, ? extends U> initial,
            BiFunction<? super U, ? super T, ? extends U> reduction) {

        this.input = input;
        this.initialTransformation = initial;
        this.reduction = reduction;
    }

    @Override
    protected final Subscription subscribeToInputs() {
        return subscribeTo(input, i -> {
            event = hasEvent
                    ? reduction.apply(event, i)
                    : initialTransformation.apply(i);
            hasEvent = true;
            emit(event);
        });
    }
}