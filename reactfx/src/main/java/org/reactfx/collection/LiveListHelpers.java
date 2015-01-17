package org.reactfx.collection;

import java.util.Collections;
import java.util.List;

import org.reactfx.ObservableBase;
import org.reactfx.ObservableHelpers;
import org.reactfx.util.Lists;

/**
 * Trait to be mixed into {@link ObservableBase} to obtain default
 * implementation of some {@link LiveList} methods and get additional
 * helper methods.
 */
public interface LiveListHelpers<E>
extends LiveList<E>, ObservableHelpers<LiveList.Observer<? super E, ?>, QuasiListChange<? extends E>> {

    @Override
    default void addQuasiChangeObserver(QuasiChangeObserver<? super E> observer) {
        addObserver(observer);
    }

    @Override
    default void removeQuasiChangeObserver(QuasiChangeObserver<? super E> observer) {
        removeObserver(observer);
    }

    @Override
    default void addQuasiModificationObserver(QuasiModificationObserver<? super E> observer) {
        addObserver(observer);
    }

    @Override
    default void removeQuasiModificationObserver(QuasiModificationObserver<? super E> observer) {
        removeObserver(observer);
    }

    default void fireModification(QuasiListModification<? extends E> mod) {
        notifyObservers(mod.asListChange());
    }

    default QuasiListModification<E> elemReplacement(int index, E replaced) {
        return new QuasiListModificationImpl<E>(
                index, Collections.singletonList(replaced), 1);
    }

    default void fireElemReplacement(int index, E replaced) {
        fireModification(elemReplacement(index, replaced));
    }

    default QuasiListModification<E> contentReplacement(List<E> removed) {
        return new QuasiListModificationImpl<E>(0, removed, size());
    }

    default void fireContentReplacement(List<E> removed) {
        fireModification(contentReplacement(removed));
    }

    default QuasiListModification<E> elemInsertion(int index) {
        return rangeInsertion(index, 1);
    }

    default void fireElemInsertion(int index) {
        fireModification(elemInsertion(index));
    }

    default QuasiListModification<E> rangeInsertion(int index, int size) {
        return new QuasiListModificationImpl<E>(
                index, Collections.emptyList(), size);
    }

    default void fireRangeInsertion(int index, int size) {
        fireModification(rangeInsertion(index, size));
    }

    default QuasiListModification<E> elemRemoval(int index, E removed) {
        return new QuasiListModificationImpl<E>(
                index, Collections.singletonList(removed), 0);
    }

    default void fireElemRemoval(int index, E removed) {
        fireModification(elemRemoval(index, removed));
    }

    default QuasiListModification<E> rangeRemoval(int index, List<E> removed) {
        return new QuasiListModificationImpl<E>(index, removed, 0);
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
