package org.reactfx.util;

@FunctionalInterface
public interface PentaFunction<A, B, C, D, E, R> {
    R apply(A a, B b, C c, D d, E e);

    default TetraFunction<B, C, D, E, R> pApply(A a) {
        return (b, c, d, e) -> apply(a, b, c, d, e);
    }
}