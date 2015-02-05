package org.reactfx.value;

import java.util.function.Consumer;

import org.reactfx.ObservableHelpers;

interface ValHelpers<T>
extends ObservableHelpers<Consumer<? super T>, T>, Val<T> {

    @Override
    default void addInvalidationObserver(Consumer<? super T> observer) {
        addObserver(observer);
    }

    @Override
    default void removeInvalidationObserver(Consumer<? super T> observer) {
        removeObserver(observer);
    }
}
