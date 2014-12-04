package org.reactfx;


/**
 * Tri-event stream that has one or more sources (most commonly event streams,
 * but not necessarily) to which it is subscribed only when it itself has
 * at least one subscriber.
 *
 * @param <A> type of the first part of events emitted by this event stream.
 * @param <B> type of the second part of events emitted by this event stream.
 * @param <C> type of the third part of events emitted by this event stream.
 */
public abstract class LazilyBoundTriStream<A, B, C>
extends LazilyBoundStreamBase<TriSubscriber<? super A, ? super B, ? super C>>
implements TriEventStream<A, B, C> {

    protected void emit(A a, B b, C c) {
        notifyObservers(s -> s.onEvent(a, b, c));
    }
}
