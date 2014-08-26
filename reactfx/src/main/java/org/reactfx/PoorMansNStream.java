package org.reactfx;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.TriConsumer;

interface PoorMansBiStream<A, B> extends BiEventStream<A, B> {

    @Override
    default Subscription subscribe(
            BiConsumer<? super A, ? super B> subscriber,
            Consumer<? super Throwable> onError) {
        return subscribe(t -> subscriber.accept(t._1, t._2), onError);
    }
}

interface PoorMansTriStream<A, B, C> extends TriEventStream<A, B, C> {

    @Override
    default Subscription subscribe(
            TriConsumer<? super A, ? super B, ? super C> subscriber,
            Consumer<? super Throwable> onError) {
        return subscribe(t -> subscriber.accept(t._1, t._2, t._3), onError);
    }
}