package org.reactfx;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.ListHelper;

/**
 * Base class for observable objects. This abstract class implements:
 * <ol>
 *   <li><b>Observer management:</b> adding and removing observers.</li>
 *   <li><b>Lazy binding to inputs.</b> An observable has 0 or more inputs,
 *   most commonly, but not necessarily, other observables. Lazy binding to
 *   inputs means that the observable observes its inputs only when it is
 *   itself being observed.</li>
 *   <li><b>Observer notification.</b></li>
 * </ol>
 *
 * @param <O> type of the observer
 * @param <T> type of observed values
 */
abstract class ObservableBase<O, T> {
    private ListHelper<O> observers = null;
    private Subscription inputSubscription = null;
    private PendingNotifications<O, T> pendingNotifications;

    ObservableBase(EmptyPendingNotifications<O, T> pendingNotifications) {
        this.pendingNotifications = pendingNotifications;
    }

    public ObservableBase() {
        this(EmptyNonRecursivePN.empty());
    }

    /**
     * Starts observing this observable's input(s), if any.
     * This method is called when the number of observers goes from 0 to 1.
     * This method is called <em>before</em> {@link #newObserver(Object)}
     * is called for the first observer.
     * @return subscription used to stop observing inputs. The subscription
     * is unsubscribed (i.e. input observation stops) when the number of
     * observers goes down to 0.
     */
    protected abstract Subscription bindToInputs();

    /**
     * Runs the given action. If {@code action} does not throw an exception,
     * returns {@code true}. If {@code action} throws an exception, then the
     * implementation may either let that exception propagate, or handle the
     * exception and return {@code false}.
     * @param action action to execute. May throw an exception.
     */
    protected abstract boolean runUnsafeAction(Runnable action);

    protected final boolean isBound() {
        return inputSubscription != null;
    }

    protected final int getObserverCount() {
        return ListHelper.size(observers);
    }

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
     * Called for each new observer.
     * Overriding this method is a convenient way for subclasses
     * to handle this event, for example to publish some initial events.
     *
     * <p>This method is called <em>after</em> the
     * {@link #bindToInputs()} method.</p>
     */
    protected void newObserver(O observer) {
        // default implementation is empty
    }

    protected final Subscription observe(O observer) {
        observers = ListHelper.add(observers, observer);
        if(ListHelper.size(observers) == 1) {
            runUnsafeAction(() -> inputSubscription = bindToInputs());
        }
        newObserver(observer);

        return () -> unobserve(observer);
    }

    private void unobserve(O observer) {
        observers = ListHelper.remove(observers, observer);
        if(ListHelper.isEmpty(observers) && inputSubscription != null) {
            runUnsafeAction(() -> {
                inputSubscription.unsubscribe();
                inputSubscription = null;
            });
        }
    }
}
