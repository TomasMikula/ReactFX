package org.reactfx;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

class AccumulativeEventStream<T, A> extends SuspendableEventStreamBase<T> {
    private final Function<? super T, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super T, ? extends A> accumulation;
    private final Function<? super A, List<T>> deconstruction;

    private boolean hasValue = false;
    private A accum = null;

    AccumulativeEventStream(
            EventStream<T> source,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction) {
        super(source);
        this.initialTransformation = initialTransformation;
        this.accumulation = accumulation;
        this.deconstruction = deconstruction;
    }

    @Override
    protected void handleEventWhenSuspended(T event) {
        if(hasValue) {
            accum = accumulation.apply(accum, event);
        } else {
            accum = initialTransformation.apply(event);
            hasValue = true;
        }
    }

    @Override
    protected void onResume() {
        if(hasValue) {
            List<T> toEmit = deconstruction.apply(accum);
            reset();
            for(T t: toEmit) {
                emit(t);
            }
        }
    }

    @Override
    protected void reset() {
        hasValue = false;
        accum = null;
    }
}
