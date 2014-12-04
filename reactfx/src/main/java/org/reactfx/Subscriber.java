package org.reactfx;

import java.util.function.Consumer;

@FunctionalInterface
public interface Subscriber<T> extends ErrorHandler {
    void onEvent(T event);

    static <T> Subscriber<T> create(
            Consumer<? super T> onEvent,
            Consumer<? super Throwable> onError) {

        return new Subscriber<T>() {

            @Override
            public void onEvent(T event) {
                onEvent.accept(event);
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        };
    }

    static <T> Subscriber<T> fromErrorHandler(
            Consumer<? super Throwable> onError) {

        return new Subscriber<T>() {

            @Override
            public void onEvent(Object event) {
                // do nothing
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        };
    }
}
