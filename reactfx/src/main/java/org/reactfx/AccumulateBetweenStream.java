package org.reactfx;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

class AccumulateBetweenStream<T, A> extends EventStreamBase<T> {
    private final EventStream<T> source;
    private final EventStream<?> ticks;
    private final Function<? super T, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super T, ? extends A> accumulation;
    private final Function<? super A, List<T>> deconstruction;

    private boolean hasValue = false;
    private A accum = null;

    public AccumulateBetweenStream(
            EventStream<T> source,
            EventStream<?> ticks,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction) {
        this.source = source;
        this.ticks = ticks;
        this.initialTransformation = initialTransformation;
        this.accumulation = accumulation;
        this.deconstruction = deconstruction;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = subscribeTo(source, this::handleEvent);
        Subscription s2 = subscribeTo(ticks, this::handleTick);
        return s1.and(s2).and(this::reset);
    }

    private void handleEvent(T event) {
        if(hasValue) {
            accum = accumulation.apply(accum, event);
        } else {
            accum = initialTransformation.apply(event);
            hasValue = true;
        }
    }

    private void handleTick(Object tick) {
        if(hasValue) {
            List<T> toEmit = deconstruction.apply(accum);
            reset();
            for(T t: toEmit) {
                emit(t);
            }
        }
    }

    private void reset() {
        hasValue = false;
        accum = null;
    }
}