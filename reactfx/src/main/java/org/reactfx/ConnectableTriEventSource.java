package org.reactfx;

import org.reactfx.util.Tuple3;

public final class ConnectableTriEventSource<A, B, C>
extends ConnectableEventSourceBase<TriSubscriber<? super A, ? super B, ? super C>>
implements TriEventStream<A, B, C>, TriConnectable<A, B, C> {

    public void push(A a, B b, C c) {
        notifyObservers(s -> s.onEvent(a, b, c));
    }

    @Override
    public final Subscription connectTo(
            EventStream<? extends Tuple3<A, B, C>> source) {
        return newInput(source, src -> subscribeTo(src, t -> push(t._1, t._2, t._3)));
    }

    @Override
    public final Subscription connectToTri(
            TriEventStream<? extends A, ? extends B, ? extends C> source) {
        return newInput(source, src -> subscribeToTri(src, this::push));
    }
}
