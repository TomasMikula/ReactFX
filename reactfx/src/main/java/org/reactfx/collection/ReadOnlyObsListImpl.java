package org.reactfx.collection;

import java.util.Collection;

import javafx.collections.ObservableList;

interface ReadOnlyObsListImpl<E> extends ObservableList<E>, ReadOnlyListImpl<E> {

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