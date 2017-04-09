package org.reactfx.util;

/**
 * An object that can be combined with the same type via {@link #reduce(Object, Object)}.
 */
public interface Semigroup<T> {
    T reduce(T left, T right);
}