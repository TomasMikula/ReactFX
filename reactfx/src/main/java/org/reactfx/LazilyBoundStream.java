package org.reactfx;

import java.util.function.Consumer;

/**
 * Event stream that has one or more sources (most commonly event streams,
 * but not necessarily) to which it is subscribed only when it itself has
 * at least one subscriber.
 *
 * @param <T> type of events emitted by this event stream.
 */
public abstract class LazilyBoundStream<T>
extends EventStreamBase<T> {
    private Subscription subscription = null;

    public LazilyBoundStream() {
        super();
    }

    LazilyBoundStream(EmptyPendingNotifications<Subscriber<? super T>, T> pn) {
        super(pn);
    }

    protected void emit(T value) {
        notifyObservers(Subscriber::onEvent, value);
    }

    protected abstract Subscription subscribeToInputs();

    @Override
    protected final void firstObserver() {
        try {
            subscription = subscribeToInputs();
        } catch(Throwable t) {
            reportError(t);
        }
    }

    @Override
    protected final void noObservers() {
        try {
            subscription.unsubscribe();
            subscription = null;
        } catch(Throwable t) {
            reportError(t);
        }
    }

    protected final boolean isBound() {
        return subscription != null;
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
}