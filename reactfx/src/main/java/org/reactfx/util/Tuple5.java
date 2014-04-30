package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

public class Tuple5<A, B, C, D, E> {
    public final A _1;
    public final B _2;
    public final C _3;
    public final D _4;
    public final E _5;

    public Tuple5(A a, B b, C c, D d, E e) {
        _1 = a;
        _2 = b;
        _3 = c;
        _4 = d;
        _5 = e;
    }

    public Tuple5<A, B, C, D, E> update1(A a) {
        return t(a, _2, _3, _4, _5);
    }

    public Tuple5<A, B, C, D, E> update2(B b) {
        return t(_1, b, _3, _4, _5);
    }

    public Tuple5<A, B, C, D, E> update3(C c) {
        return t(_1, _2, c, _4, _5);
    }

    public Tuple5<A, B, C, D, E> update4(D d) {
        return t(_1, _2, _3, d, _5);
    }

    public Tuple5<A, B, C, D, E> update5(E e) {
        return t(_1, _2, _3, _4, e);
    }
}
