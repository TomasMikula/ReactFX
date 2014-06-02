package org.reactfx.util;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either<L, R> {

    public static <L, R> Either<L, R> left(L l) {
        return new Either<L, R>() {

            @Override
            public boolean isLeft() { return true; }

            @Override
            public boolean isRight() { return false; }

            @Override
            public L getLeft() { return l; }

            @Override
            public R getRight() { throw new NoSuchElementException(); }

            @Override
            public Optional<L> asLeft() { return Optional.of(l); }

            @Override
            public Optional<R> asRight() { return Optional.empty(); }

            @Override
            public void ifLeft(Consumer<L> f) { f.accept(l); }

            @Override
            public void ifRight(Consumer<R> f) { /* do nothing */ }

            @Override
            public <L2> Either<L2, R> mapLeft(Function<L, L2> f) {
                return left(f.apply(l));
            }

            @Override
            public <R2> Either<L, R2> mapRight(Function<R, R2> f) {
                return left(l);
            }

            @Override
            public <L2, R2> Either<L2, R2> map(
                    Function<L, L2> f,
                    Function<R, R2> g) {
                return left(f.apply(l));
            }
        };
    }

    public static <L, R> Either<L, R> right(R r) {
        return new Either<L, R>() {

            @Override
            public boolean isLeft() { return false; }

            @Override
            public boolean isRight() { return true; }

            @Override
            public L getLeft() { throw new NoSuchElementException(); }

            @Override
            public R getRight() { return r; }

            @Override
            public Optional<L> asLeft() { return Optional.empty(); }

            @Override
            public Optional<R> asRight() { return Optional.of(r); }

            @Override
            public void ifLeft(Consumer<L> f) { /* do nothing */ }

            @Override
            public void ifRight(Consumer<R> f) { f.accept(r); }

            @Override
            public <L2> Either<L2, R> mapLeft(Function<L, L2> f) {
                return right(r);
            }

            @Override
            public <R2> Either<L, R2> mapRight(Function<R, R2> f) {
                return right(f.apply(r));
            }

            @Override
            public <L2, R2> Either<L2, R2> map(
                    Function<L, L2> f,
                    Function<R, R2> g) {
                return right(g.apply(r));
            }
        };
    }

    // private constructor to prevent subclassing
    private Either() {}

    public abstract boolean isLeft();
    public abstract boolean isRight();
    public abstract L getLeft();
    public abstract R getRight();
    public abstract Optional<L> asLeft();
    public abstract Optional<R> asRight();
    public abstract void ifLeft(Consumer<L> f);
    public abstract void ifRight(Consumer<R> f);
    public abstract <L2> Either<L2, R> mapLeft(Function<L, L2> f);
    public abstract <R2> Either<L, R2> mapRight(Function<R, R2> f);
    public abstract <L2, R2> Either<L2, R2> map(Function<L, L2> f, Function<R, R2> g);
}
