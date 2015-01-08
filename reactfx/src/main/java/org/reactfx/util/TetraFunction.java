package org.reactfx.util;


@FunctionalInterface
public interface TetraFunction<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);

    default TriFunction<B, C, D, R> pApply(A a) {
        return (b, c, d) -> apply(a, b, c, d);
    }
}