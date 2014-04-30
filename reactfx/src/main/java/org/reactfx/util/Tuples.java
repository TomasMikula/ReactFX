package org.reactfx.util;

public class Tuples {

    public static <A, B> Tuple2<A, B> t(A a, B b) {
        return new Tuple2<>(a, b);
    }
}
