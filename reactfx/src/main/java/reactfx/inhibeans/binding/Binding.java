package reactfx.inhibeans.binding;

import reactfx.inhibeans.value.ObservableValue;

public interface Binding<T>
extends javafx.beans.binding.Binding<T>, ObservableValue<T> {}
