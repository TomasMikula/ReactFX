package org.reactfx;


public class BiEventSource<A, B>
extends EventStreamBase<BiSubscriber<? super A, ? super B>>
implements BiEventStream<A, B>, BiEventSink<A, B> {

    @Override
    public final void push(A a, B b) {
        notifyObservers(s -> s.onEvent(a, b));
    }
}
