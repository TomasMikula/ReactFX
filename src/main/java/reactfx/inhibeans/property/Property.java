package reactfx.inhibeans.property;

import reactfx.inhibeans.value.ObservableValue;

public interface Property<T>
extends javafx.beans.property.Property<T>, ObservableValue<T> {}
