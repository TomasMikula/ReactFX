package org.reactfx;

import java.util.function.Consumer;

/**
 * Trait to be mixed into {@link ObservableBase} to obtain default
 * implementation of some {@link EventStream} methods and get additional
 * helper methods.
 */
public interface EventStreamHelpers<T>
extends EventStream<T>, ObservableHelpers<Consumer<? super T>, T> {

    default void emit(T value) {
        notifyObservers(value);
    }

    @Override
    default Subscription subscribe(Consumer<? super T> subscriber) {
        return observe(subscriber);
    }
}
