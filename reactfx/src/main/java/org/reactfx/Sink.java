package org.reactfx;

public interface Sink<T> {
    void push(T value);
}
