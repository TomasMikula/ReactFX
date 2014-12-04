package org.reactfx;


interface PoorMansBiStream<A, B> extends BiEventStream<A, B> {

    @Override
    default Subscription subscribe(
            BiSubscriber<? super A, ? super B> subscriber) {
        return subscribe(subscriber.toSubscriber());
    }
}

interface PoorMansTriStream<A, B, C> extends TriEventStream<A, B, C> {

    @Override
    default Subscription subscribe(
            TriSubscriber<? super A, ? super B, ? super C> subscriber) {
        return subscribe(subscriber.toSubscriber());
    }
}