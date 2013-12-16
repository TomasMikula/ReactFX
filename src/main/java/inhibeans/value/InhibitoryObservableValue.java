package inhibeans.value;

import javafx.beans.value.ObservableValue;

public interface InhibitoryObservableValue<T> extends ObservableValue<T> {

    void block();

    void release();
}
