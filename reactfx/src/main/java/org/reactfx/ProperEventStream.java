package org.reactfx;

import java.util.function.Consumer;

import org.reactfx.util.NotificationAccumulator;

/**
 * Trait to be mixed into {@link ObservableBase} to obtain default
 * implementation of some {@link EventStream} methods on top of
 * {@linkplain Observable} methods and get additional helper methods for
 * <em>proper</em> event streams implemented as default methods on top of
 * {@linkplain ProperObservable} methods.
 */
public interface ProperEventStream<T>
extends EventStream<T>, ProperObservable<Consumer<? super T>, T> {

    default void emit(T value) {
        notifyObservers(value);
    }

    @Override
    default NotificationAccumulator<Consumer<? super T>, T, ?> defaultNotificationAccumulator() {
        return NotificationAccumulator.nonAccumulativeStreamNotifications();
    }
}
