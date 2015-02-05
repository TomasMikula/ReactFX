package org.reactfx.value;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

class VarWrapper<T> extends ValWrapper<T, Property<T>> implements Var<T> {

    VarWrapper(Property<T> delegate) {
        super(delegate);
    }

    @Override
    public void bind(ObservableValue<? extends T> other) {
        getDelegate().bind(other);
    }

    @Override
    public boolean isBound() {
        return getDelegate().isBound();
    }

    @Override
    public void unbind() {
        getDelegate().unbind();
    }

    @Override
    public void setValue(T value) {
        getDelegate().setValue(value);
    }
}