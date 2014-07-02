package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class Tuple2<A, B> {
    public final A _1;
    public final B _2;

    Tuple2(A a, B b) {
        _1 = a;
        _2 = b;
    }

    public Tuple2<A, B> update1(A a) {
        return t(a, _2);
    }

    public Tuple2<A, B> update2(B b) {
        return t(_1, b);
    }

    public <T> T map(BiFunction<? super A, ? super B, ? extends T> f) {
        return f.apply(_1, _2);
    }

    public boolean test(BiPredicate<? super A, ? super B> f) {
        return f.test(_1, _2);
    }

    public void exec(BiConsumer<? super A, ? super B> f) {
        f.accept(_1, _2);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Tuple2) {
            Tuple2<?, ?> that = (Tuple2<?, ?>) other;
            return Objects.equals(this._1, that._1)
                    && Objects.equals(this._2, that._2);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2);
    }

    @Override
    public String toString() {
        return "("
                + Objects.toString(_1) + ", "
                + Objects.toString(_2)
                + ")";
    }
}
