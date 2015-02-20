package org.reactfx;

import org.reactfx.util.Experimental;

@Experimental
public abstract class SuspenderBase<O, T, S extends Suspendable>
extends ObservableBase<O, T> implements Suspender<S> {
    private final S suspendable;

    protected SuspenderBase(S suspendable) {
        this.suspendable = suspendable;
    }

    /**
     * Use in subclasses instead of {@link #notifyObservers(Object)} in order
     * to suspend the associated {@linkplain Suspendable} while notifying
     * observers.
     */
    protected final void notifyObserversWhileSuspended(T notification) {
        try(Guard g = suspendable.suspend()) {
            notifyObservers(notification);
        }
    }

    @Override
    public final S getSuspendable() {
        return suspendable;
    }
}