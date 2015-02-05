package org.reactfx.value;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;

import org.reactfx.Subscription;

class ValWrapper<T, D extends ObservableValue<T>> extends ValBase<T> {
    private final D delegate;

    ValWrapper(D delegate) {
        this.delegate = delegate;
    }

    D getDelegate() {
        return delegate;
    }

    @Override
    protected Subscription connect() {
        InvalidationListener listener = obs -> invalidate();
        delegate.addListener(listener);
        return () -> delegate.removeListener(listener);
    }

    @Override
    protected T computeValue() {
        return delegate.getValue();
    }
}