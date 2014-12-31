package org.reactfx.inhibeans.collection;

import org.reactfx.collection.ObsList;

@Deprecated
public final class Collections {

    // private constructor to prevent instantiation
    private Collections() {}

    /**
     * Creates an ObservableList wrapper that is able to temporarily block
     * list change notifications.
     * @param delegate the underlying observable list.
     * @deprecated Use {@link ObsList#suspendable(javafx.collections.ObservableList)} instead.
     */
    @Deprecated
    public static <E> ObservableList<E> wrap(javafx.collections.ObservableList<E> delegate) {
        return ObsList.suspendable(delegate);
    }
}
