package org.reactfx;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

class AccumulateUntilLaterStream<T, A> extends EventStreamBase<T> {
    private final EventStream<T> source;
    private final Function<? super T, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super T, ? extends A> accumulation;
    private final Function<? super A, List<T>> deconstruction;
    private final Executor eventThreadExecutor;

    private boolean hasValue = false;
    private A accum = null;

    public AccumulateUntilLaterStream(
            EventStream<T> source,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction,
            Executor eventThreadExecutor) {
        this.source = source;
        this.initialTransformation = initialTransformation;
        this.accumulation = accumulation;
        this.deconstruction = deconstruction;
        this.eventThreadExecutor = eventThreadExecutor;
    }

    @Override
    protected Subscription bindToInputs() {
        return source.subscribe(this::handleEvent);
    }

    private void handleEvent(T event) {
        if(hasValue) {
            accum = accumulation.apply(accum, event);
            // emission already scheduled
        } else {
            accum = initialTransformation.apply(event);
            hasValue = true;
            eventThreadExecutor.execute(this::emitAccum);
        }
    }

    private void emitAccum() {
        assert hasValue;
        hasValue = false;
        List<T> toEmit = deconstruction.apply(accum);
        accum = null;
        for(T t: toEmit) {
            emit(t);
        }
    }
}