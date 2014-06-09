package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

import java.util.Objects;

public class Tuple5<A, B, C, D, E> {
    public final A _1;
    public final B _2;
    public final C _3;
    public final D _4;
    public final E _5;

    Tuple5(A a, B b, C c, D d, E e) {
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

    public <T> T map(PentaFunction<? super A, ? super B, ? super C, ? super D, ? super E, ? extends T> f) {
        return f.apply(_1, _2, _3, _4, _5);
    }

    public boolean test(PentaPredicate<? super A, ? super B, ? super C, ? super D, ? super E> f) {
        return f.test(_1, _2, _3, _4, _5);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Tuple5) {
            Tuple5<?, ?, ?, ?, ?> that = (Tuple5<?, ?, ?, ?, ?>) other;
            return Objects.equals(this._1, that._1)
                    && Objects.equals(this._2, that._2)
                    && Objects.equals(this._3, that._3)
                    && Objects.equals(this._4, that._4)
                    && Objects.equals(this._5, that._5);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3, _4, _5);
    }

    @Override
    public String toString() {
        return "("
                + Objects.toString(_1) + ", "
                + Objects.toString(_2) + ", "
                + Objects.toString(_3) + ", "
                + Objects.toString(_4) + ", "
                + Objects.toString(_5)
                + ")";
    }
}
