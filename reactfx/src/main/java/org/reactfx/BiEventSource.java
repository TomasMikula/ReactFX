package org.reactfx;

import java.util.function.BiConsumer;

public class BiEventSource<A, B>
extends EventStreamBase<BiConsumer<? super A, ? super B>>
implements BiEventStream<A, B>, BiEventSink<A, B> {

    @Override
    public final void push(A a, B b) {
        forEachSubscriber(s -> s.accept(a, b));
    }
}
