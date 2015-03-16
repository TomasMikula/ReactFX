package org.reactfx.util;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Try<T> extends Either<Throwable, T> {

    static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Try<T> failure(Throwable thrown) {
        return new Failure<>(thrown);
    }

    static <T> Try<T> tryGet(Callable<? extends T> f) {
        try {
            return success(f.call());
        } catch(Throwable t) {
            return failure(t);
        }
    }

    default boolean isSuccess() { return isRight(); }
    default boolean isFailure() { return isLeft(); }
    default T get() { return getRight(); }
    default Throwable getFailure() { return getLeft(); }
    default Optional<T> toOptional() { return asRight(); }
    default void ifSuccess(Consumer<? super T> f) { ifRight(f); }
    default void ifFailure(Consumer<? super Throwable> f) { ifLeft(f); }

    T getOrElse(T fallback);
    T getOrElse(Supplier<T> fallback);
    Try<T> orElse(Try<T> fallback);
    Try<T> orElse(Supplier<Try<T>> fallback);
    Try<T> orElseTry(Callable<? extends T> fallback);
    Try<T> recover(Function<Throwable, Optional<T>> f);
    <U> Try<U> map(Function<? super T, ? extends U> f);
    <U> Try<U> flatMap(Function<? super T, Try<U>> f);
}

class Success<T> extends Right<Throwable, T> implements Try<T> {

    public Success(T value) { super(value); }

    @Override
    public T getOrElse(T fallback) { return getRight(); }

    @Override
    public T getOrElse(Supplier<T> fallback) { return getRight(); }

    @Override
    public Try<T> orElse(Try<T> fallback) { return this; }

    @Override
    public Try<T> orElse(Supplier<Try<T>> fallback) { return this; }

    @Override
    public Try<T> orElseTry(Callable<? extends T> fallback) { return this; }

    @Override
    public Try<T> recover(Function<Throwable, Optional<T>> f) { return this; }

    @Override
    public <U> Try<U> map(Function<? super T, ? extends U> f) {
        return new Success<>(f.apply(get()));
    }

    @Override
    public <U> Try<U> flatMap(Function<? super T, Try<U>> f) {
        return f.apply(get());
    }

    @Override
    public String toString() {
        return "success(" + get() + ")";
    }
}

class Failure<T> extends Left<Throwable, T> implements Try<T> {

    public Failure(Throwable thrown) { super(thrown); }

    @Override
    public T getOrElse(T fallback) { return fallback; }

    @Override
    public T getOrElse(Supplier<T> fallback) { return fallback.get(); }

    @Override
    public Try<T> orElse(Try<T> fallback) { return fallback; }

    @Override
    public Try<T> orElse(Supplier<Try<T>> fallback) { return fallback.get(); }

    @Override
    public Try<T> orElseTry(Callable<? extends T> fallback) { return Try.tryGet(fallback); }

    @Override
    public Try<T> recover(Function<Throwable, Optional<T>> f) {
        Optional<T> recovered = f.apply(getFailure());
        if(recovered.isPresent()) {
            return new Success<>(recovered.get());
        } else {
            return this;
        }
    }

    @Override
    public <U> Try<U> map(Function<? super T, ? extends U> f) {
        return new Failure<>(getFailure());
    }

    @Override
    public <U> Try<U> flatMap(Function<? super T, Try<U>> f) {
        return new Failure<>(getFailure());
    }

    @Override
    public String toString() {
        return "failure(" + getFailure() + ")";
    }
}