package org.reactfx.value;

import java.util.function.Consumer;

import javafx.beans.value.ObservableValue;

import org.reactfx.Subscription;

class VarFromVal<T> extends ProxyVal<T, T> implements Var<T> {
    private final Consumer<T> setter;

    private Subscription binding = null;

    VarFromVal(Val<T> underlying, Consumer<T> setter) {
        super(underlying);
        this.setter = setter;
    }

    @Override
    public T getValue() {
        return getUnderlyingObservable().getValue();
    }

    @Override
    protected Consumer<? super T> adaptObserver(Consumer<? super T> observer) {
        return observer; // no adaptation needed
    }

    @Override
    public void bind(ObservableValue<? extends T> observable) {
        unbind();
        binding = Val.observeChanges(
                observable,
                (obs, oldVal, newVal) -> setValue(newVal));
        setValue(observable.getValue());
    }

    @Override
    public void unbind() {
        if(binding != null) {
            binding.unsubscribe();
            binding = null;
        }
    }

    @Override
    public boolean isBound() {
        return binding != null;
    }

    @Override
    public void setValue(T value) {
        setter.accept(value);
    }

}
