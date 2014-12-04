package org.reactfx;


public class TriEventSource<A, B, C>
extends EventStreamBase<TriSubscriber<? super A, ? super B, ? super C>>
implements TriEventStream<A, B, C>, TriEventSink<A, B, C> {

    @Override
    public final void push(A a, B b, C c) {
        notifyObservers(s -> s.onEvent(a, b, c));
    }
}
