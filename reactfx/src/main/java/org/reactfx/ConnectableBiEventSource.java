package org.reactfx;

import java.util.function.BiConsumer;

import org.reactfx.util.Tuple2;

public final class ConnectableBiEventSource<A, B>
extends ConnectableEventSourceBase<BiConsumer<? super A, ? super B>>
implements ConnectableBiEventStream<A, B>, ConnectableBiEventSink<A, B> {

    @Override
    public void push(A a, B b) {
        forEachSubscriber(s -> s.accept(a, b));
    }

    @Override
    public final Subscription connectTo(
            EventStream<? extends Tuple2<A, B>> source) {
        return newInput(source, src -> subscribeTo(src, t -> push(t._1, t._2)));
    }

    @Override
    public final Subscription connectToBi(
            BiEventStream<? extends A, ? extends B> source) {
        return newInput(source, src -> subscribeToBi(src, this::push));
    }
}
