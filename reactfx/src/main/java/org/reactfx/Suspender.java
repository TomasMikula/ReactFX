package org.reactfx;

import org.reactfx.util.Experimental;

/**
 * Suspender is an object capable of suspending another suspendable object.
 *
 * @param <S> type of the suspendable object this suspender suspends.
 */
@Experimental
public interface Suspender<S extends Suspendable> {
    S getSuspendable();
}
