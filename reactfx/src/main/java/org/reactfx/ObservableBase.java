package org.reactfx;

import java.util.function.Consumer;

import org.reactfx.util.ListHelper;

/**
 *
 * @param <O> type of the observer
 */
abstract class ObservableBase<O> {

    private ListHelper<O> observers = null;
    private boolean unnotifiedObservers = false;

    protected final int getObserverCount() {
        return ListHelper.size(observers);
    }

    protected abstract void runUnsafeAction(Runnable action);

    protected final void notifyObservers(Consumer<O> notification) {
        if(unnotifiedObservers) {
            runUnsafeAction(() -> {
                throw new IllegalStateException("Cannot recursively notify"
                        + " observers before all observers were notified of"
                        + " the previous event");
            });
            return;
        }

        if(ListHelper.size(observers) > 1) {
            // prevent recursion when there are 2 or more observers
            unnotifiedObservers = true;
        }
        try {
            ListHelper.forEach(observers, o -> {
                runUnsafeAction(() -> notification.accept(o));
            });
        } finally {
            unnotifiedObservers = false;
        }
    }

    /**
     * Executes action for each observer, regardless of recursion state.
     * If {@code action} throws an exception for one observer, it will not
     * be called for any subsequent observers and the exception will be
     * propagated by this method.
     * @param action action to execute for each observer.
     */
    protected final void forEachObserver(Consumer<O> action) {
        ListHelper.forEach(observers, o -> action.accept(o));
    }

    /**
     * Called when the number of observers goes from 0 to 1.
     * Overriding this method is a convenient way for subclasses
     * to handle this event.
     *
     * <p>This method is called <em>before</em> the
     * {@link #newObserver(Object)} method.</p>
     */
    protected void firstObserver() {
        // default implementation is empty
    }

    /**
     * Called for each new observer.
     * Overriding this method is a convenient way for subclasses
     * to handle this event, for example to publish some initial events.
     *
     * <p>This method is called <em>after</em> the
     * {@link #firstObserver()} method.</p>
     */
    protected void newObserver(O observer) {
        // default implementation is empty
    }

    /**
     * Called when the number of observers goes down to 0.
     * Overriding this method is a convenient way for subclasses
     * to handle this event.
     */
    protected void noObservers() {
        // default implementation is empty
    }

    protected final Subscription observe(O observer) {

        observers = ListHelper.add(observers, observer);
        if(ListHelper.size(observers) == 1) {
            firstObserver();
        }
        newObserver(observer);

        return () -> unobserve(observer);
    }

    private void unobserve(O observer) {
        observers = ListHelper.remove(observers, observer);
        if(ListHelper.isEmpty(observers)) {
            noObservers();
        }
    }
}
