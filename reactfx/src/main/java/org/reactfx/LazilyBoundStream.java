package org.reactfx;


/**
 * Event stream that has one or more sources (most commonly event streams,
 * but not necessarily) to which it is subscribed only when it itself has
 * at least one subscriber.
 *
 * @param <T> type of events emitted by this event stream.
 */
public abstract class LazilyBoundStream<T>
extends LazilyBoundStreamBase<Subscriber<? super T>>
implements EventStream<T> {

    protected void emit(T value) {
        notifyObservers(s -> s.onEvent(value));
    }
}
