package org.reactfx;

import static org.reactfx.util.Tuples.*;

import java.util.function.Consumer;

import org.reactfx.util.TriConsumer;
import org.reactfx.util.Tuple3;

public class TriEventSource<A, B, C>
extends EventStreamBase<TriConsumer<? super A, ? super B, ? super C>>
implements TriEventStream<A, B, C>, TriEventSink<A, B, C> {

    @Override
    public final Subscription subscribe(Consumer<? super Tuple3<A, B, C>> subscriber) {
        return subscribe((a, b, c) -> subscriber.accept(t(a, b, c)));
    }

    @Override
    public final void push(A a, B b, C c) {
        forEachSubscriber(s -> s.accept(a, b, c));
    }
}
