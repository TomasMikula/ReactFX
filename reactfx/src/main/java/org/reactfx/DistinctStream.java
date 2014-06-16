package org.reactfx;

import java.util.Objects;

import org.reactfx.util.Either;


class DistinctStream<T> extends LazilyBoundStream<T> {
    static final Object NONE = new Object();
    private final EventStream<T> input;
    private Object previous = NONE;

    public DistinctStream(EventStream<T> input) {
        this.input = input;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(input, value -> {
            Object prevToCompare = previous;
            previous = value;
            if (!Objects.equals(value, prevToCompare)) {
                emit(value);
            }
        });
    }
}

class DistinctBiStream<A, B> extends LazilyBoundBiStream<A, B> {
    private final BiEventStream<A, B> input;
    private Object previousA = DistinctStream.NONE;
    private Object previousB = DistinctStream.NONE;

    DistinctBiStream(BiEventStream<A, B> input) {
        this.input = input;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToBi(input, (a, b) -> {
            Object aPrevToCompare = previousA;
            previousA = a;
            Object bPrevToCompare = previousB;
            previousB = b;

            if (Objects.equals(a, aPrevToCompare) && Objects.equals(b, bPrevToCompare)) {
                return;
            }
            emit(a, b);
        });
    }
}

class DistinctTriStream<A, B, C> extends LazilyBoundTriStream<A, B, C> {
    private final TriEventStream<A, B, C> input;
    private Object previousA = DistinctStream.NONE;
    private Object previousB = DistinctStream.NONE;
    private Object previousC = DistinctStream.NONE;

    DistinctTriStream(TriEventStream<A, B, C> input) {
        this.input = input;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToTri(input, (a, b, c) -> {
            Object aPrevToCompare = previousA;
            previousA = a;
            Object bPrevToCompare = previousB;
            previousB = b;
            Object cPrevToCompare = previousC;
            previousC = c;

            if (Objects.equals(a, aPrevToCompare) &&
                Objects.equals(b, bPrevToCompare) &&
                Objects.equals(c, cPrevToCompare)) {
                return;
            }
            emit(a, b, c);
        });
    }
}

class DistinctEitherStream<L, R>
extends DistinctStream<Either<L, R>>
implements EitherEventStream<L, R> {

    public DistinctEitherStream(EventStream<Either<L, R>> input) {
        super(input);
    }
}