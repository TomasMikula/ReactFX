package org.reactfx;

import org.reactfx.util.Either;

class OrStream<L, R> extends LazilyBoundStream<Either<L, R>> implements EitherEventStream<L, R> {
    private final EventStream<? extends L> left;
    private final EventStream<? extends R> right;

    public OrStream(EventStream<? extends L> left, EventStream<? extends R> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return Subscription.multi(
                subscribeTo(left, l -> emit(Either.<L, R>left(l))),
                subscribeTo(right, r -> emit(Either.<L, R>right(r))));
    }
}