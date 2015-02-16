package org.reactfx;

import javafx.beans.value.ObservableBooleanValue;

import org.reactfx.value.ValBase;

abstract class SuspendableBoolean extends ValBase<Boolean>
implements ObservableBooleanValue, Suspendable {

    private int suspenders = 0;

    @Override
    public final Guard suspend() {
        if(++suspenders == 1) {
            invalidate();
        }

        return ((Guard) this::release).closeableOnce();
    }

    private void release() {
        assert suspenders > 0;
        if(--suspenders == 0) {
            invalidate();
        }
    }

    public EventStream<?> yeses() {
        return EventStreams.valuesOf(this).filterMap(val -> !val, val -> null);
    }

    public EventStream<?> noes() {
        return EventStreams.valuesOf(this).filterMap(val -> val, val -> null);
    }

    protected final boolean isSuspended() {
        return suspenders > 0;
    }

    @Override
    protected final Subscription connect() {
        return Subscription.EMPTY;
    }

    @Override
    protected final Boolean computeValue() {
        return get();
    }
}