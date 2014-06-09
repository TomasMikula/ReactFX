package org.reactfx.util;

@FunctionalInterface
public interface TetraPredicate<A, B, C, D> {
    boolean test(A a, B b, C c, D d);
}