package org.reactfx;

import java.util.function.BiConsumer;

/**
 * Bi-event stream that has one or more sources (most commonly event streams,
 * but not necessarily) to which it is subscribed only when it itself has
 * at least one subscriber.
 *
 * @param <A> type of the first part of events emitted by this event stream.
 * @param <B> type of the second part of events emitted by this event stream.
 */
public abstract class LazilyBoundBiStream<A, B>
extends LazilyBoundStreamBase<BiConsumer<? super A, ? super B>>
implements BiEventStream<A, B> {

    protected void emit(A a, B b) {
        forEachSubscriber(s -> s.accept(a, b));
    }
}
