package org.reactfx;

/**
 * Observable boolean value that is normally {@code false}, but is {@code true}
 * when suspended.
 */
public class SuspendableNo extends SuspendableBoolean {

    @Override
    public boolean get() {
        return isSuspended();
    }
}
