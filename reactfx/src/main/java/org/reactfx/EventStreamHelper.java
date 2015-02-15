package org.reactfx;

import java.util.function.Consumer;

/**
 * Trait to be mixed into {@link Observable} to obtain default implementation
 * of some {@link EventStream} methods on top of {@linkplain Observable}
 * methods.
 */
interface EventStreamHelper<T>
extends EventStream<T>, Observable<Consumer<? super T>> {

    @Override
    default Subscription subscribe(Consumer<? super T> subscriber) {
        return observe(subscriber);
    }
}