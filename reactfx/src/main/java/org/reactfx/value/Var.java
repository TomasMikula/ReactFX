package org.reactfx.value;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;

public interface Var<T> extends Val<T>, Property<T> {
    static <T> Var<T> newSimpleVar(T initialValue) {
        return new SimpleVar<>(initialValue);
    }

    static <T> SuspendableVar<T> suspendable(Property<T> p) {
        if(p instanceof SuspendableVar) {
            return (SuspendableVar<T>) p;
        } else {
            Var<T> var = p instanceof Var
                    ? (Var<T>) p
                    : new VarWrapper<>(p);
            return new SuspendableVarWrapper<>(var);
        }
    }

    @Override
    default void bindBidirectional(Property<T> other) {
        Bindings.bindBidirectional(this, other);
    }

    @Override
    default void unbindBidirectional(Property<T> other) {
        Bindings.unbindBidirectional(this, other);
    }

    @Override
    default Object getBean() {
        return null;
    }

    @Override
    default String getName() {
        return null;
    }

    @Override
    default SuspendableVar<T> suspendable() {
        return suspendable(this);
    }
}