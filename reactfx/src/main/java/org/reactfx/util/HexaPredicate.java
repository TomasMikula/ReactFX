package org.reactfx.util;

@FunctionalInterface
public interface HexaPredicate<A, B, C, D, E, F> {
    boolean test(A a, B b, C c, D d, E e, F f);
}