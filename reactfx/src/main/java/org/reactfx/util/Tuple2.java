package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

public class Tuple2<A, B> {
    public final A _1;
    public final B _2;

    public Tuple2(A a, B b) {
        _1 = a;
        _2 = b;
    }

    public Tuple2<A, B> update1(A a) {
        return t(a, _2);
    }

    public Tuple2<A, B> update2(B b) {
        return t(_1, b);
    }
}
