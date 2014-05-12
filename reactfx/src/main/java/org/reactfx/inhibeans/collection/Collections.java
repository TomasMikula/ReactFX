package org.reactfx.inhibeans.collection;

public class Collections {

    public static <E> ObservableList<E> wrap(javafx.collections.ObservableList<E> delegate) {
        return new ObservableListWrapper<>(delegate);
    }
}
