package org.reactfx.value;

import java.util.function.Consumer;

import org.reactfx.RigidObservable;

class ConstVal<T>
extends RigidObservable<Consumer<? super T>>
implements Val<T> {
    private final T value;

    ConstVal(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }
}
