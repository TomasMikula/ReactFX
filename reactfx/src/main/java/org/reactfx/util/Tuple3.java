package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

public class Tuple3<A, B, C> {
    public final A _1;
    public final B _2;
    public final C _3;

    public Tuple3(A a, B b, C c) {
        _1 = a;
        _2 = b;
        _3 = c;
    }

    public Tuple3<A, B, C> update1(A a) {
        return t(a, _2, _3);
    }

    public Tuple3<A, B, C> update2(B b) {
        return t(_1, b, _3);
    }

    public Tuple3<A, B, C> update3(C c) {
        return t(_1, _2, c);
    }
}
