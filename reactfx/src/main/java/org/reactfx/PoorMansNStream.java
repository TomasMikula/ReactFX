package org.reactfx;

import java.util.function.BiConsumer;

import org.reactfx.util.TriConsumer;

interface PoorMansBiStream<A, B> extends BiEventStream<A, B> {

    @Override
    default Subscription subscribe(BiConsumer<? super A, ? super B> subscriber) {
        return subscribe(t -> subscriber.accept(t._1, t._2));
    }
}

interface PoorMansTriStream<A, B, C> extends TriEventStream<A, B, C> {

    @Override
    default Subscription subscribe(TriConsumer<? super A, ? super B, ? super C> subscriber) {
        return subscribe(t -> subscriber.accept(t._1, t._2, t._3));
    }
}