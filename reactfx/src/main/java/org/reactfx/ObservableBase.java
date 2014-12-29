package org.reactfx;

import java.util.function.Consumer;

import org.reactfx.util.ListHelper;
import org.reactfx.util.NotificationAccumulator;

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
 * @param <T> type of produced values
 */
public abstract class ObservableBase<O, T> implements ObservableHelpers<O, T> {
    private ListHelper<O> observers = null;
    private Subscription inputSubscription = null;
    private final NotificationAccumulator<O, T, ?> pendingNotifications;

    protected ObservableBase(NotificationAccumulator<O, T, ?> pendingNotificationsImpl) {
        this.pendingNotifications = pendingNotificationsImpl;
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

    protected final boolean isBound() {
        return inputSubscription != null;
    }

    protected final int getObserverCount() {
        return ListHelper.size(observers);
    }

    @Override
    public final void notifyObservers(T event) {
        enqueueNotifications(event);
        notifyObservers();
    }

    protected final void enqueueNotifications(T event) {
        // may throw if pendingNotifications not empty and recursion not allowed
        pendingNotifications.addAll(ListHelper.iterator(observers), event);
    }

    protected final void notifyObservers() {
        try {
            while(!pendingNotifications.isEmpty()) {
                pendingNotifications.takeOne().run(); // run() may throw
            }
        } finally {
            pendingNotifications.clear();
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

    @Override
    public final Subscription observe(O observer) {
        addObserver(observer);
        return () -> removeObserver(observer);
    }

    @Override
    public final void addObserver(O observer) {
        observers = ListHelper.add(observers, observer);
        if(ListHelper.size(observers) == 1) {
            inputSubscription = bindToInputs();
        }
        newObserver(observer);
    }

    @Override
    public final void removeObserver(O observer) {
        observers = ListHelper.remove(observers, observer);
        if(ListHelper.isEmpty(observers) && inputSubscription != null) {
            inputSubscription.unsubscribe();
            inputSubscription = null;
        }
    }

    @Override
    public int hashCode() {
        return defaultHashCode();
    }

    @Override
    public boolean equals(Object o) {
        return defaultEquals(o);
    }

    @Override
    public String toString() {
        return defaultToString();
    }
}
