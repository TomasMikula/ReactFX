package org.reactfx.inhibeans.binding;

import org.reactfx.inhibeans.value.ObservableValue;
import org.reactfx.value.Val;

@Deprecated
public interface Binding<T>
extends javafx.beans.binding.Binding<T>, ObservableValue<T> {

    /**
     * @deprecated Use {@link Val#suspendable(javafx.beans.value.ObservableValue)}.
     */
    @Deprecated
    public static <T> Binding<T> wrap(javafx.beans.value.ObservableValue<T> source) {
        return new ObjectBinding<T>() {
            { bind(source); }

            @Override
            protected T computeValue() { return source.getValue(); }
        };
    }
}
