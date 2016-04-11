package org.reactfx.util;

public interface Semigroup<T> {
    T reduce(T left, T right);
}