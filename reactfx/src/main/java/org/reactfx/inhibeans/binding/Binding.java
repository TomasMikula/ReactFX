package org.reactfx.inhibeans.binding;

import org.reactfx.inhibeans.value.ObservableValue;

public interface Binding<T>
extends javafx.beans.binding.Binding<T>, ObservableValue<T> {}
