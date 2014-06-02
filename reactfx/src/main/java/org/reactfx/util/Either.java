package org.reactfx.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either<L, R> {

    private static class Left<L, R> extends Either<L, R> {
        private final L value;

        public Left(L value) { this.value = value; }

        @Override
        public boolean isLeft() { return true; }

        @Override
        public boolean isRight() { return false; }

        @Override
        public L getLeft() { return value; }

        @Override
        public R getRight() { throw new NoSuchElementException(); }

        @Override
        public Optional<L> asLeft() { return Optional.of(value); }

        @Override
        public Optional<R> asRight() { return Optional.empty(); }

        @Override
        public void ifLeft(Consumer<L> f) { f.accept(value); }

        @Override
        public void ifRight(Consumer<R> f) { /* do nothing */ }

        @Override
        public <L2> Either<L2, R> mapLeft(Function<L, L2> f) {
            return left(f.apply(value));
        }

        @Override
        public <R2> Either<L, R2> mapRight(Function<R, R2> f) {
            return left(value);
        }

        @Override
        public <L2, R2> Either<L2, R2> map(
                Function<L, L2> f,
                Function<R, R2> g) {
            return left(f.apply(value));
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof Left) {
                Left<?, ?> that = (Left<?, ?>) other;
                return Objects.equals(this.value, that.value);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "left(" + value + ")";
        }
    }

    private static class Right<L, R> extends Either<L, R> {
        private final R value;

        public Right(R value) { this.value = value; }

        @Override
        public boolean isLeft() { return false; }

        @Override
        public boolean isRight() { return true; }

        @Override
        public L getLeft() { throw new NoSuchElementException(); }

        @Override
        public R getRight() { return value; }

        @Override
        public Optional<L> asLeft() { return Optional.empty(); }

        @Override
        public Optional<R> asRight() { return Optional.of(value); }

        @Override
        public void ifLeft(Consumer<L> f) { /* do nothing */ }

        @Override
        public void ifRight(Consumer<R> f) { f.accept(value); }

        @Override
        public <L2> Either<L2, R> mapLeft(Function<L, L2> f) {
            return right(value);
        }

        @Override
        public <R2> Either<L, R2> mapRight(Function<R, R2> f) {
            return right(f.apply(value));
        }

        @Override
        public <L2, R2> Either<L2, R2> map(
                Function<L, L2> f,
                Function<R, R2> g) {
            return right(g.apply(value));
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof Right) {
                Right<?, ?> that = (Right<?, ?>) other;
                return Objects.equals(this.value, that.value);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "right(" + value + ")";
        }
    }

    public static <L, R> Either<L, R> left(L l) {
        return new Left<>(l);
    }

    public static <L, R> Either<L, R> right(R r) {
        return new Right<>(r);
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
