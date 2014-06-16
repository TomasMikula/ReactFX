package org.reactfx;

import org.reactfx.util.TriConsumer;

public class TriEventSource<A, B, C>
extends EventStreamBase<TriConsumer<? super A, ? super B, ? super C>>
implements TriEventStream<A, B, C>, TriEventSink<A, B, C> {

    @Override
    public final void push(A a, B b, C c) {
        forEachSubscriber(s -> s.accept(a, b, c));
    }
}
