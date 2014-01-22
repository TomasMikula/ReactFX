package inhibeans.property;

import inhibeans.value.ObservableValue;

public interface Property<T>
extends javafx.beans.property.Property<T>, ObservableValue<T> {}
