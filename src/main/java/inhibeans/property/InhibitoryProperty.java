package inhibeans.property;

import inhibeans.value.ObservableValue;

public interface InhibitoryProperty<T>
extends javafx.beans.property.Property<T>, ObservableValue<T> {}
