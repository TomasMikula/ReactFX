package org.reactfx.collection;

import org.reactfx.Observable;

/**
 * Trait to be mixed into {@link Observable} to obtain default implementation
 * of some {@link LiveList} methods on top of {@linkplain Observable} methods.
 */
public interface LiveListHelper<E>
extends LiveList<E>, Observable<LiveList.Observer<? super E, ?>> {

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
}