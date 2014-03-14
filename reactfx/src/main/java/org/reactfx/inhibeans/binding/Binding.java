package org.reactfx.inhibeans.binding;

import org.reactfx.inhibeans.value.ObservableValue;

public interface Binding<T>
extends javafx.beans.binding.Binding<T>, ObservableValue<T> {

    public static <T> Binding<T> wrap(javafx.beans.value.ObservableValue<T> source) {
        return new ObjectBinding<T>() {
            { bind(source); }

            @Override
            protected T computeValue() { return source.getValue(); }
        };
    }
}
