package inhibeans.property;

import inhibeans.value.InhibitoryObservableValue;
import javafx.beans.property.Property;

public interface InhibitoryProperty<T>
extends Property<T>, InhibitoryObservableValue<T> {
}
