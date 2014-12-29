package org.reactfx;

import org.reactfx.util.NotificationAccumulator;


/**
 * Base class for event streams.
 *
 * @param <T> type of events emitted by this event stream.
 */
public abstract class EventStreamBase<T>
extends ObservableBase<Subscriber<? super T>, T>
implements EventStream<T> {

    public EventStreamBase() {
        this(NotificationAccumulator.nonRecursiveStreamNotifications());
    }

    EventStreamBase(NotificationAccumulator<Subscriber<? super T>, T> pn) {
        super(pn);
    }

    protected final void emit(T value) {
        notifyObservers(value);
    }

    @Override
    public final Subscription subscribe(Subscriber<? super T> subscriber) {
        return observe(subscriber);
    }
}
