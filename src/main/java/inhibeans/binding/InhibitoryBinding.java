package inhibeans.binding;

import inhibeans.value.InhibitoryObservableValue;
import javafx.beans.binding.Binding;

public interface InhibitoryBinding<T>
extends Binding<T>, InhibitoryObservableValue<T> {
}
