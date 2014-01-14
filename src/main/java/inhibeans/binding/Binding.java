package inhibeans.binding;

import inhibeans.value.ObservableValue;

public interface Binding<T>
extends javafx.beans.binding.Binding<T>, ObservableValue<T> {}
