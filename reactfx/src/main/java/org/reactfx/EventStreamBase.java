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
implements EventStreamHelpers<T> {

    public EventStreamBase() {
        this(NotificationAccumulator.nonAccumulativeStreamNotifications());
    }

    EventStreamBase(NotificationAccumulator<Consumer<? super T>, T, ?> pn) {
        super(pn);
    }
}
