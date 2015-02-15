package org.reactfx.value;

import javafx.beans.value.ObservableValue;

class SuspendableVarWrapper<T>
extends SuspendableValWrapper<T>
implements SuspendableVar<T> {
    private final Var<T> delegate;

    protected SuspendableVarWrapper(Var<T> p) {
        super(p);
        this.delegate = p;
    }

    @Override
    public void bind(ObservableValue<? extends T> other) {
        delegate.bind(other);
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public void unbind() {
        delegate.unbind();
    }

    @Override
    public void setValue(T value) {
        delegate.setValue(value);
    }
}
