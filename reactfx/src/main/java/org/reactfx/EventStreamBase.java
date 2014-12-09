package org.reactfx;


/**
 *
 * @param <T> type of events
 */
public abstract class EventStreamBase<T>
extends ObservableBase<Subscriber<? super T>, T>
implements EventStream<T> {

    private boolean reporting = false;

    public EventStreamBase() {
        super();
    }

    EventStreamBase(EmptyPendingNotifications<Subscriber<? super T>, T> pn) {
        super(pn);
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

    @Override
    public final Subscription subscribe(Subscriber<? super T> subscriber) {
        return observe(subscriber);
    }
}
