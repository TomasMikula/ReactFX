package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

public class Tuple6<A, B, C, D, E, F> {
    public final A _1;
    public final B _2;
    public final C _3;
    public final D _4;
    public final E _5;
    public final F _6;

    public Tuple6(A a, B b, C c, D d, E e, F f) {
        _1 = a;
        _2 = b;
        _3 = c;
        _4 = d;
        _5 = e;
        _6 = f;
    }

    public Tuple6<A, B, C, D, E, F> update1(A a) {
        return t(a, _2, _3, _4, _5, _6);
    }

    public Tuple6<A, B, C, D, E, F> update2(B b) {
        return t(_1, b, _3, _4, _5, _6);
    }

    public Tuple6<A, B, C, D, E, F> update3(C c) {
        return t(_1, _2, c, _4, _5, _6);
    }

    public Tuple6<A, B, C, D, E, F> update4(D d) {
        return t(_1, _2, _3, d, _5, _6);
    }

    public Tuple6<A, B, C, D, E, F> update5(E e) {
        return t(_1, _2, _3, _4, e, _6);
    }

    public Tuple6<A, B, C, D, E, F> update6(F f) {
        return t(_1, _2, _3, _4, _5, f);
    }
}
