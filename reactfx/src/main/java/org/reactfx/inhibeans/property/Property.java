package org.reactfx.inhibeans.property;

import org.reactfx.inhibeans.value.ObservableValue;

public interface Property<T>
extends javafx.beans.property.Property<T>, ObservableValue<T> {}
