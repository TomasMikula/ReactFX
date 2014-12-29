package org.reactfx.collection;

import java.util.Collections;
import java.util.List;

import org.reactfx.ObservableBase;
import org.reactfx.ObservableHelpers;
import org.reactfx.util.Lists;

/**
 * Trait to be mixed into {@link ObservableBase} to obtain default
 * implementation of some {@link ObsList} methods and get additional
 * helper methods.
 */
public interface ObsListHelpers<E>
extends ObsList<E>, ObservableHelpers<ObsList.Observer<? super E, ?>, ListChange<? extends E>> {

    @Override
    default void addChangeObserver(ChangeObserver<? super E> observer) {
        addObserver(observer);
    }

    @Override
    default void removeChangeObserver(ChangeObserver<? super E> observer) {
        removeObserver(observer);
    }

    @Override
    default void addModificationObserver(ModificationObserver<? super E> observer) {
        addObserver(observer);
    }

    @Override
    default void removeModificationObserver(ModificationObserver<? super E> observer) {
        removeObserver(observer);
    }

    default void fireModification(TransientListModification<? extends E> mod) {
        notifyObservers(mod.asListChange());
    }

    default TransientListModification<E> elemReplacement(int index, E replaced) {
        return new TransientListModificationImpl<E>(
                this, index, index+1, Collections.singletonList(replaced));
    }

    default void fireElemReplacement(int index, E replaced) {
        fireModification(elemReplacement(index, replaced));
    }

    default TransientListModification<E> contentReplacement(List<E> removed) {
        return new TransientListModificationImpl<E>(this, 0, size(), removed);
    }

    default void fireContentReplacement(List<E> removed) {
        fireModification(contentReplacement(removed));
    }

    default TransientListModification<E> elemInsertion(int index) {
        return rangeInsertion(index, 1);
    }

    default void fireElemInsertion(int index) {
        fireModification(elemInsertion(index));
    }

    default TransientListModification<E> rangeInsertion(int index, int size) {
        return new TransientListModificationImpl<E>(
                this, index, index + size, Collections.emptyList());
    }

    default void fireRangeInsertion(int index, int size) {
        fireModification(rangeInsertion(index, size));
    }

    default TransientListModification<E> elemRemoval(int index, E removed) {
        return new TransientListModificationImpl<E>(
                this, index, index, Collections.singletonList(removed));
    }

    default void fireElemRemoval(int index, E removed) {
        fireModification(elemRemoval(index, removed));
    }

    default TransientListModification<E> rangeRemoval(int index, List<E> removed) {
        return new TransientListModificationImpl<E>(
                this, index, index, removed);
    }

    default void fireRemoveRange(int index, List<E> removed) {
        fireModification(rangeRemoval(index, removed));
    }

    @Override
    default int defaultHashCode() {
        return Lists.hashCode(this);
    }

    @Override
    default boolean defaultEquals(Object o) {
        return Lists.equals(this, o);
    }

    @Override
    default String defaultToString() {
        return Lists.toString(this);
    }
}
