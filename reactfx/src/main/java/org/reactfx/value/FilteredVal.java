package org.reactfx.value;

import java.util.function.Predicate;

import javafx.beans.value.ObservableValue;

import org.reactfx.Subscription;


class FilteredVal<T> extends ValBase<T> {
    private final ObservableValue<T> src;
    private final Predicate<? super T> p;

    FilteredVal(
            ObservableValue<T> src,
            Predicate<? super T> p) {
        this.src = src;
        this.p = p;
    }

    @Override
    protected T computeValue() {
        T val = src.getValue();
        return (val != null && p.test(val)) ? val : null;
    }

    @Override
    protected Subscription connect() {
        return Val.observeInvalidations(src, obs -> invalidate());
    }
}
