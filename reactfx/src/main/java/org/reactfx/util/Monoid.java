package org.reactfx.util;

public interface Monoid<T> {
    T unit();
    T reduce(T left, T right);
}