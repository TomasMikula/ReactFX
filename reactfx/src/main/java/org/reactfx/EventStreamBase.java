package org.reactfx;

import java.util.function.Consumer;

import org.reactfx.util.NotificationAccumulator;


/**
 * Base class for event streams.
 *
 * @param <T> type of events emitted by this event stream.
 */
public abstract class EventStreamBase<T>
extends ObservableBase<Consumer<? super T>, T>
implements EventStream<T> {

    public EventStreamBase() {
        this(NotificationAccumulator.nonRecursiveStreamNotifications());
    }

    EventStreamBase(NotificationAccumulator<Consumer<? super T>, T> pn) {
        super(pn);
    }

    protected final void emit(T value) {
        notifyObservers(value);
    }

    @Override
    public final Subscription subscribe(Consumer<? super T> subscriber) {
        return observe(subscriber);
    }
}
