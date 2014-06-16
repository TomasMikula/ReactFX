package org.reactfx;

/**
 * Base class for an event stream that has one or more sources (most commonly
 * event streams, but not necessarily) to which it is subscribed only when it
 * itself has at least one subscriber.
 *
 * @param <S> type of the subscriber
 */
public abstract class LazilyBoundStreamBase<S> extends EventStreamBase<S> {
    private Subscription subscription = null;

    protected abstract Subscription subscribeToInputs();

    @Override
    protected final void firstSubscriber() {
        try {
            subscription = subscribeToInputs();
        } catch(Throwable t) {
            reportError(t);
        }
    }

    @Override
    protected final void noSubscribers() {
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
}