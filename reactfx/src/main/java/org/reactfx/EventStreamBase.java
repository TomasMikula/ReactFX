package org.reactfx;


/**
 *
 * @param <S> type of the subscriber
 */
public abstract class EventStreamBase<S extends ErrorHandler> extends ObservableBase<S> {

    private boolean reporting = false;

    @Override
    protected final void runUnsafeAction(Runnable action) {
        try {
            action.run();
        } catch(Throwable t) {
            reportError(t);
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

    public final Subscription subscribe(S subscriber) {
        return observe(subscriber);
    }
}
