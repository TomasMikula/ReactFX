package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

import java.util.Objects;

public class Tuple3<A, B, C> {
    public final A _1;
    public final B _2;
    public final C _3;

    Tuple3(A a, B b, C c) {
        _1 = a;
        _2 = b;
        _3 = c;
    }

    public A get1() { return _1; }
    public B get2() { return _2; }
    public C get3() { return _3; }

    public Tuple3<A, B, C> update1(A a) {
        return t(a, _2, _3);
    }

    public Tuple3<A, B, C> update2(B b) {
        return t(_1, b, _3);
    }

    public Tuple3<A, B, C> update3(C c) {
        return t(_1, _2, c);
    }

    public <T> T map(TriFunction<? super A, ? super B, ? super C, ? extends T> f) {
        return f.apply(_1, _2, _3);
    }

    public boolean test(TriPredicate<? super A, ? super B, ? super C> f) {
        return f.test(_1, _2, _3);
    }

    public void exec(TriConsumer<? super A, ? super B, ? super C> f) {
        f.accept(_1, _2, _3);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Tuple3) {
            Tuple3<?, ?, ?> that = (Tuple3<?, ?, ?>) other;
            return Objects.equals(this._1, that._1)
                    && Objects.equals(this._2, that._2)
                    && Objects.equals(this._3, that._3);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3);
    }

    @Override
    public String toString() {
        return "("
                + Objects.toString(_1) + ", "
                + Objects.toString(_2) + ", "
                + Objects.toString(_3)
                + ")";
    }
}
