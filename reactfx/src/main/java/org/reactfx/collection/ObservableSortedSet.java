package org.reactfx.collection;

import java.util.SortedSet;

import javafx.collections.ObservableSet;

/**
 * A {@link SortedSet} that is also {@linkplain ObservableSet observable}.
 * Implementations of this interface provide a read-only {@link #listView} of
 * their contents, which is also sorted.
 *
 * @see ObservableSortedArraySet
 */
public interface ObservableSortedSet<E> extends ObservableSet<E>, SortedSet<E> {
    /**
     * A read-only {@link LiveList} view of this
     * {@link ObservableSortedSet}'s contents. It will issue events whenever
     * items are added to or removed from this {@code ObservableSortedSet},
     * and when their sort order changes.
     */
    LiveList<E> listView();
}