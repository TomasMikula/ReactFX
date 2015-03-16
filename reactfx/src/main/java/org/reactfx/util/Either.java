package org.reactfx.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Either<L, R> {

    static <L, R> Either<L, R> left(L l) {
        return new Left<>(l);
    }

    static <L, R> Either<L, R> right(R r) {
        return new Right<>(r);
    }

    static <L, R> Either<L, R> leftOrNull(Optional<L> l) {
        return leftOrDefault(l, null);
    }

    static <L, R> Either<L, R> rightOrNull(Optional<R> r) {
        return rightOrDefault(r, null);
    }

    static <L, R> Either<L, R> leftOrDefault(Optional<L> l, R r) {
        return l.isPresent() ? left(l.get()) : right(r);
    }

    static <L, R> Either<L, R> rightOrDefault(Optional<R> r, L l) {
        return r.isPresent() ? right(r.get()) : left(l);
    }

    boolean isLeft();
    boolean isRight();
    L getLeft();
    R getRight();
    L toLeft(Function<? super R, ? extends L> f);
    R toRight(Function<? super L, ? extends R> f);
    Optional<L> asLeft();
    Optional<R> asRight();
    void ifLeft(Consumer<? super L> f);
    void ifRight(Consumer<? super R> f);
    void exec(
            Consumer<? super L> ifLeft,
            Consumer<? super R> ifRight);
    <L2> Either<L2, R> mapLeft(
            Function<? super L, ? extends L2> f);
    <R2> Either<L, R2> mapRight(
            Function<? super R, ? extends R2> f);
    <L2, R2> Either<L2, R2> map(
            Function<? super L, ? extends L2> f,
            Function<? super R, ? extends R2> g);
    <L2, R2> Either<L2, R2> flatMap(
            Function<? super L, Either<L2, R2>> f,
            Function<? super R, Either<L2, R2>> g);
    <L2> Either<L2, R> flatMapLeft(
            Function<? super L, Either<L2, R>> f);
    <R2> Either<L, R2> flatMapRight(
            Function<? super R, Either<L, R2>> f);
    <T> T unify(
            Function<? super L, ? extends T> f,
            Function<? super R, ? extends T> g);
}


class Left<L, R> implements Either<L, R> {
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
    public L toLeft(Function<? super R, ? extends L> f) {
        return value;
    }

    @Override
    public R toRight(Function<? super L, ? extends R> f) {
        return f.apply(value);
    }

    @Override
    public Optional<L> asLeft() { return Optional.of(value); }

    @Override
    public Optional<R> asRight() { return Optional.empty(); }

    @Override
    public void ifLeft(Consumer<? super L> f) { f.accept(value); }

    @Override
    public void ifRight(Consumer<? super R> f) { /* do nothing */ }

    @Override
    public void exec(
            Consumer<? super L> ifLeft,
            Consumer<? super R> ifRight) {
        ifLeft.accept(value);
    }

    @Override
    public <L2> Either<L2, R> mapLeft(Function<? super L, ? extends L2> f) {
        return new Left<>(f.apply(value));
    }

    @Override
    public <R2> Either<L, R2> mapRight(Function<? super R, ? extends R2> f) {
        return new Left<>(value);
    }

    @Override
    public <L2, R2> Either<L2, R2> map(
            Function<? super L, ? extends L2> f,
            Function<? super R, ? extends R2> g) {
        return new Left<>(f.apply(value));
    }

    @Override
    public <L2, R2> Either<L2, R2> flatMap(
            Function<? super L, Either<L2, R2>> f,
            Function<? super R, Either<L2, R2>> g) {
        return f.apply(value);
    }

    @Override
    public <L2> Either<L2, R> flatMapLeft(
            Function<? super L, Either<L2, R>> f) {
        return f.apply(value);
    }

    @Override
    public <R2> Either<L, R2> flatMapRight(
            Function<? super R, Either<L, R2>> f) {
        return new Left<>(value);
    }

    @Override
    public <T> T unify(
            Function<? super L, ? extends T> f,
            Function<? super R, ? extends T> g) {
        return f.apply(value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public final boolean equals(Object other) {
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


class Right<L, R> implements Either<L, R> {
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
    public L toLeft(Function<? super R, ? extends L> f) {
        return f.apply(value);
    }

    @Override
    public R toRight(Function<? super L, ? extends R> f) {
        return value;
    }

    @Override
    public Optional<L> asLeft() { return Optional.empty(); }

    @Override
    public Optional<R> asRight() { return Optional.of(value); }

    @Override
    public void ifLeft(Consumer<? super L> f) { /* do nothing */ }

    @Override
    public void ifRight(Consumer<? super R> f) { f.accept(value); }

    @Override
    public void exec(
            Consumer<? super L> ifLeft,
            Consumer<? super R> ifRight) {
        ifRight.accept(value);
    }

    @Override
    public <L2> Either<L2, R> mapLeft(Function<? super L, ? extends L2> f) {
        return new Right<>(value);
    }

    @Override
    public <R2> Either<L, R2> mapRight(Function<? super R, ? extends R2> f) {
        return new Right<>(f.apply(value));
    }

    @Override
    public <L2, R2> Either<L2, R2> map(
            Function<? super L, ? extends L2> f,
            Function<? super R, ? extends R2> g) {
        return new Right<>(g.apply(value));
    }

    @Override
    public <L2, R2> Either<L2, R2> flatMap(
            Function<? super L, Either<L2, R2>> f,
            Function<? super R, Either<L2, R2>> g) {
        return g.apply(value);
    }

    @Override
    public <L2> Either<L2, R> flatMapLeft(
            Function<? super L, Either<L2, R>> f) {
        return new Right<>(value);
    }

    @Override
    public <R2> Either<L, R2> flatMapRight(
            Function<? super R, Either<L, R2>> f) {
        return f.apply(value);
    }

    @Override
    public <T> T unify(
            Function<? super L, ? extends T> f,
            Function<? super R, ? extends T> g) {
        return g.apply(value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public final boolean equals(Object other) {
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