package org.reactfx;

import org.reactfx.util.Either;

class GuardedStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final Guardian guardian;

    public GuardedStream(EventStream<T> source, Guardian... guardians) {
        this.source = source;
        this.guardian = Guardian.combine(guardians);
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(source, evt -> {
            try(Guard g = guardian.guard()) {
                emit(evt);
            }
        });
    }
}

class GuardedBiStream<A, B> extends LazilyBoundBiStream<A, B> {
    private final BiEventStream<A, B> source;
    private final Guardian guardian;

    public GuardedBiStream(BiEventStream<A, B> source, Guardian... guardians) {
        this.source = source;
        this.guardian = Guardian.combine(guardians);
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToBi(source, (a, b) -> {
            try(Guard g = guardian.guard()) {
                emit(a, b);
            }
        });
    }
}

class GuardedTriStream<A, B, C> extends LazilyBoundTriStream<A, B, C> {
    private final TriEventStream<A, B, C> source;
    private final Guardian guardian;

    public GuardedTriStream(TriEventStream<A, B, C> source, Guardian... guardians) {
        this.source = source;
        this.guardian = Guardian.combine(guardians);
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToTri(source, (a, b, c) -> {
            try(Guard g = guardian.guard()) {
                emit(a, b, c);
            }
        });
    }
}

@Deprecated
class GuardedEitherStream<L, R>
extends GuardedStream<Either<L, R>>
implements EitherEventStream<L, R> {

    public GuardedEitherStream(
            EventStream<Either<L, R>> source,
            Guardian[] guardians) {
        super(source, guardians);
    }
}