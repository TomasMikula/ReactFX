package org.reactfx.util;

@FunctionalInterface
public interface PentaPredicate<A, B, C, D, E> {
    boolean test(A a, B b, C c, D d, E e);
}