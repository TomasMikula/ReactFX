package org.reactfx;


@FunctionalInterface
public interface Subscriber<T> {
    void onEvent(T event);
}
