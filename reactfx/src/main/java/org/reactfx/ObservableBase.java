package org.reactfx;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.ListHelper;

/**
 *
 * @param <O> type of the observer
 * @param <T> type of observed values
 */
abstract class ObservableBase<O, T> {
    private ListHelper<O> observers = null;
    private PendingNotifications<O, T> pendingNotifications;

    ObservableBase(EmptyPendingNotifications<O, T> pendingNotifications) {
        this.pendingNotifications = pendingNotifications;
    }

    public ObservableBase() {
        this(EmptyNonRecursivePN.empty());
    }

    protected final int getObserverCount() {
        return ListHelper.size(observers);
    }

    /**
     * Runs the given action. If {@code action} does not throw an exception,
     * returns {@code true}. If {@code action} throws an exception, then the
     * implementation may either let that exception propagate, or handle the
     * exception and return {@code false}.
     * @param action action to execute. May throw an exception.
     */
    protected abstract boolean runUnsafeAction(Runnable action);

    protected final void notifyObservers(BiConsumer<O, T> notifier, T event) {
        try {
            boolean added = runUnsafeAction(() -> {
                // may throw if pendingNotifications not empty and recursion not allowed
                pendingNotifications = pendingNotifications.addAll(ListHelper.iterator(observers), event);
            });
            if(!added) return;

            while(!pendingNotifications.isEmpty()) {
                pendingNotifications.takeOne().exec((observer, evt, rest) -> {
                    pendingNotifications = rest;
                    runUnsafeAction(() -> notifier.accept(observer, evt)); // may throw
                });
            }
        } finally {
            pendingNotifications.close(); // clears all pending notifications
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
