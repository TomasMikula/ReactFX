package org.reactfx.collection;

import java.util.Collection;

import javafx.collections.ObservableList;

/**
 * Trait to be mixed into implementations of unmodifiable {@link LiveList}s.
 * Provides default implementations of mutating list methods.
 */
public interface UnmodifiableByDefaultLiveList<E>
extends ObservableList<E>, UnmodifiableByDefaultList<E> {

    @Override
    default boolean addAll(@SuppressWarnings("unchecked") E... elems) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void remove(int from, int to) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean removeAll(@SuppressWarnings("unchecked") E... elems) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean retainAll(@SuppressWarnings("unchecked") E... elems) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean setAll(@SuppressWarnings("unchecked") E... elems) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean setAll(Collection<? extends E> elems) {
        throw new UnsupportedOperationException();
    }
}