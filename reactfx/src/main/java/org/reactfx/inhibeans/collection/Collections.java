package org.reactfx.inhibeans.collection;

public final class Collections {

    // private constructor to prevent instantiation
    private Collections() {}

    /**
     * Creates an ObservableList wrapper that is able to temporarily block
     * list change notifications.
     * @param delegate the underlying observable list.
     * @return
     */
    public static <E> ObservableList<E> wrap(javafx.collections.ObservableList<E> delegate) {
        return new ObservableListWrapper<>(delegate);
    }
}
