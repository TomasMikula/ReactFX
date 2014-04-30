package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

public class Tuple4<A, B, C, D> {
    public final A _1;
    public final B _2;
    public final C _3;
    public final D _4;

    public Tuple4(A a, B b, C c, D d) {
        _1 = a;
        _2 = b;
        _3 = c;
        _4 = d;
    }

    public Tuple4<A, B, C, D> update1(A a) {
        return t(a, _2, _3, _4);
    }

    public Tuple4<A, B, C, D> update2(B b) {
        return t(_1, b, _3, _4);
    }

    public Tuple4<A, B, C, D> update3(C c) {
        return t(_1, _2, c, _4);
    }

    public Tuple4<A, B, C, D> update4(D d) {
        return t(_1, _2, _3, d);
    }
}
