package org.reactfx.value;

import java.util.function.Function;

import javafx.beans.value.ObservableValue;

import org.reactfx.Subscription;

public class MappedVal<T, U> extends ValBase<U> {
    private final ObservableValue<T> src;
    private final Function<? super T, ? extends U> f;

    MappedVal(
            ObservableValue<T> src,
            Function<? super T, ? extends U> f) {
        this.src = src;
        this.f = f;
    }

    @Override
    protected U computeValue() {
        T baseVal = src.getValue();
        return baseVal != null ? f.apply(baseVal) : null;
    }

    @Override
    protected Subscription connect() {
        return Val.observeInvalidations(src, obs -> invalidate());
    }
}
