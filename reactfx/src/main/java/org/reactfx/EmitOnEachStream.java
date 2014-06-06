package org.reactfx;

import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;

class EmitOnEachStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final EventStream<?> impulse;

    private boolean hasValue = false;
    private T value = null;

    public EmitOnEachStream(EventStream<T> source, EventStream<?> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = source.subscribe(v -> {
            hasValue = true;
            value = v;
        });

        Subscription s2 = impulse.subscribe(i -> {
            if(hasValue) {
                emit(value);
            }
        });

        return s1.and(s2);
    }
}

class EmitOnEachBiStream<A, B>
extends EmitOnEachStream<Tuple2<A, B>>
implements PoorMansBiStream<A, B> {

    public EmitOnEachBiStream(
            EventStream<Tuple2<A, B>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}

class EmitOnEachTriStream<A, B, C>
extends EmitOnEachStream<Tuple3<A, B, C>>
implements PoorMansTriStream<A, B, C> {

    public EmitOnEachTriStream(
            EventStream<Tuple3<A, B, C>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}

class EmitOnEachEitherStream<L, R>
extends EmitOnEachStream<Either<L, R>>
implements EitherEventStream<L, R> {

    public EmitOnEachEitherStream(
            EventStream<Either<L, R>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}