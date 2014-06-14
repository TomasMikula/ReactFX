package org.reactfx;

import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;

class RepeatOnStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final EventStream<?> impulse;

    private boolean hasValue = false;
    private T value = null;

    public RepeatOnStream(EventStream<T> source, EventStream<?> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = subscribeTo(source, v -> {
            hasValue = true;
            value = v;
            emit(v);
        });

        Subscription s2 = subscribeTo(impulse, i -> {
            if(hasValue) {
                emit(value);
            }
        });

        return s1.and(s2);
    }
}

class RepeatOnBiStream<A, B>
extends RepeatOnStream<Tuple2<A, B>>
implements PoorMansBiStream<A, B> {

    public RepeatOnBiStream(
            EventStream<Tuple2<A, B>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}

class RepeatOnTriStream<A, B, C>
extends RepeatOnStream<Tuple3<A, B, C>>
implements PoorMansTriStream<A, B, C> {

    public RepeatOnTriStream(
            EventStream<Tuple3<A, B, C>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}

class RepeatOnEitherStream<L, R>
extends RepeatOnStream<Either<L, R>>
implements EitherEventStream<L, R> {

    public RepeatOnEitherStream(
            EventStream<Either<L, R>> source,
            EventStream<?> impulse) {
        super(source, impulse);
    }
}