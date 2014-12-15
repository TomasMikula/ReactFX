package org.reactfx;

import java.util.function.Consumer;

import org.reactfx.util.AccuMap;


/**
 * Base class for event streams.
 * Adds support for error propagation on top of {@link ObservableBase}.
 *
 * @param <T> type of events emitted by this event stream.
 */
public abstract class EventStreamBase<T>
extends ObservableBase<Subscriber<? super T>, T>
implements EventStream<T> {

    private boolean reporting = false;

    public EventStreamBase() {
        super();
    }

    EventStreamBase(AccuMap.Empty<Subscriber<? super T>, T> pn) {
        super(pn);
    }

    protected final void emit(T value) {
        notifyObservers(Subscriber::onEvent, value);
    }

    @Override
    protected final boolean runUnsafeAction(Runnable action) {
        try {
            action.run();
            return true;
        } catch(Throwable t) {
            reportError(t);
            return false;
        }
    }

    /**
     * Notifies all registered error handlers of the given error.
     */
    protected final void reportError(Throwable thrown) {
        if(reporting) {
            // Error reporting caused another error in the same stream.
            // Don't notify handlers again (likely to cause an error again).
            // Just print the stack trace.
            thrown.printStackTrace();
        } else {
            reporting = true;
            forEachObserver(o -> {
                try {
                    o.onError(thrown);
                } catch(Throwable another) { // error handler threw an exception
                    another.printStackTrace();
                }
            });
            reporting = false;
        }
    }

    /**
     * Subscribes to the given event stream by the given subscriber and also
     * forwards errors reported by the given stream to this stream. This is
     * equivalent to {@code stream.subscribe(subscriber, this::reportError)}.
     * @return subscription used to unsubscribe {@code subscriber} from
     * {@code stream} and stop forwarding the errors.
     */
    protected final <U> Subscription subscribeTo(
            EventStream<U> stream,
            Consumer<? super U> subscriber) {
        return stream.subscribe(subscriber, this::reportError);
    }

    @Override
    public final Subscription subscribe(Subscriber<? super T> subscriber) {
        return observe(subscriber);
    }
}
