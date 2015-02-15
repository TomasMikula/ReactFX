package org.reactfx;

/**
 * An observable that does not change and does not produce any notifications.
 * @param <O> observer type accepted by this {@linkplain Observable}
 */
public abstract class RigidObservable<O> implements Observable<O> {

    /**
     * Adding an observer to a {@linkplain RigidObservable} is a no-op.
     */
    @Override
    public final void addObserver(O observer) {
        // no-op
    }

    /**
     * Removing an observer from a {@linkplain RigidObservable} is a no-op.
     */
    @Override
    public final void removeObserver(O observer) {
        // no-op
    }

    /**
     * Returns an empty {@linkplain Subscription} ({@link Subscription#EMPTY}).
     */
    @Override
    public final Subscription observe(O observer) {
        return Subscription.EMPTY;
    }
}
