package inhibeans.property;

import javafx.beans.property.Property;

public interface InhibitoryProperty<T> extends Property<T> {

    void block();

    void release();
}
