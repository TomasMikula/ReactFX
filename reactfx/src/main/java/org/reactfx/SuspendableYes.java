package org.reactfx;

/**
 * Observable boolean value that is normally {@code true}, but is {@code false}
 * when suspended.
 */
public class SuspendableYes extends SuspendableBoolean {

    @Override
    public boolean get() {
        return !isSuspended();
    }
}
