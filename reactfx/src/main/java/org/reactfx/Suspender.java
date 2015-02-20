package org.reactfx;

/**
 * Suspender is an object capable of suspending another suspendable object.
 *
 * @param <S> type of the suspendable object this suspender suspends.
 */
public interface Suspender<S extends Suspendable> {
    S getSuspendable();
}
