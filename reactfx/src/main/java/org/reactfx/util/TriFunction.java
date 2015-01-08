package org.reactfx.util;

import java.util.function.BiFunction;

@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);

    default BiFunction<B, C, R> pApply(A a) {
        return (b, c) -> apply(a, b, c);
    }
}