package org.reactfx.value;

import javafx.beans.value.ObservableValue;

import org.reactfx.Subscription;

class OrElse<T> extends ValBase<T> {
    private final ObservableValue<? extends T> src;
    private final ObservableValue<? extends T> other;

    private boolean trySrc; // irrelevant when not isConnected()


    OrElse(
            ObservableValue<? extends T> src,
            ObservableValue<? extends T> other) {
        this.src = src;
        this.other = other;
    }

    @Override
    protected Subscription connect() {
        trySrc = true;
        Subscription sub1 = Val.observeInvalidations(src, obs -> {
            trySrc = true;
            invalidate();
        });
        Subscription sub2 = Val.observeInvalidations(other, obs -> {
            if(!trySrc) {
                invalidate();
            }
        });
        return sub1.and(sub2);
    }

    @Override
    protected T computeValue() {
        if(!isObservingInputs()) {
            T val = src.getValue();
            return val != null ? val : other.getValue();
        } else {
            if(trySrc) {
                T val = src.getValue();
                if(val != null) {
                    return val;
                } else {
                    trySrc = false;
                }
            }
            return other.getValue();
        }
    }
}
