package org.reactfx;

import static org.reactfx.util.Tuples.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.Tuple2;

public class BiEventSource<A, B>
extends EventStreamBase<BiConsumer<? super A, ? super B>>
implements BiEventStream<A, B>, BiEventSink<A, B> {

    @Override
    public final Subscription subscribe(Consumer<? super Tuple2<A, B>> subscriber) {
        return subscribe((a, b) -> subscriber.accept(t(a, b)));
    }

    @Override
    public final void push(A a, B b) {
        forEachSubscriber(s -> s.accept(a, b));
    }
}
