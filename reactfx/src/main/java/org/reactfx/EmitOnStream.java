package org.reactfx;

import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;

class EmitOnStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final EventStream<?> impulse;

    private boolean hasValue = false;
    private T value = null;

    public EmitOnStream(EventStream<T> source, EventStream<?> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = subscribeTo(source, v -> {
            hasValue = true;
            value = v;
        });

        Subscription s2 = subscribeTo(impulse, i -> {
            if(hasValue) {
                T val = value;
                hasValue = false;
                value = null;
                emit(val);
            }
        });

        return s1.and(s2);
    }
}

class EmitOnBiStream<A, B>
extends EmitOnStream<Tuple2<A, B>>
implements PoorMansBiStream<A, B> {

    public EmitOnBiStream(
            EventStream<Tuple2<A, B>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}

class EmitOnTriStream<A, B, C>
extends EmitOnStream<Tuple3<A, B, C>>
implements PoorMansTriStream<A, B, C> {

    public EmitOnTriStream(
            EventStream<Tuple3<A, B, C>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}

class EmitOnEitherStream<L, R>
extends EmitOnStream<Either<L, R>>
implements EitherEventStream<L, R> {

    public EmitOnEitherStream(
            EventStream<Either<L, R>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}