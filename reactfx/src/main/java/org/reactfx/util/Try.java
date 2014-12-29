package org.reactfx.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Try<T> {

    private static class Success<T> extends Try<T> {
        private final T value;

        public Success(T value) { this.value = value; }

        @Override
        public boolean isSuccess() { return true; }

        @Override
        public boolean isFailure() { return false; }

        @Override
        public T get() { return value; }

        @Override
        public T getOrElse(T fallback) { return value; }

        @Override
        public T getOrElse(Supplier<T> fallback) { return value; }

        @Override
        public Throwable getFailure() { throw new NoSuchElementException(); }

        @Override
        public Try<T> orElse(Try<T> fallback) { return this; }

        @Override
        public Try<T> orElse(Supplier<Try<T>> fallback) { return this; }

        @Override
        public Try<T> recover(Function<Throwable, Optional<T>> f) { return this; }

        @Override
        public Optional<T> toOptional() { return Optional.of(value); }

        @Override
        public void ifSuccess(Consumer<? super T> f) { f.accept(value); }

        @Override
        public void ifFailure(Consumer<? super Throwable> f) { /* do nothing */ }

        @Override
        public <U> Try<U> map(Function<? super T, ? extends U> f) {
            return success(f.apply(value));
        }

        @Override
        public <U> Try<U> flatMap(Function<? super T, Try<U>> f) {
            return f.apply(value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof Success) {
                Success<?> that = (Success<?>) other;
                return Objects.equals(this.value, that.value);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "success(" + value + ")";
        }
    }

    private static class Failure<T> extends Try<T> {
        private final Throwable thrown;

        public Failure(Throwable thrown) { this.thrown = thrown; }

        @Override
        public boolean isSuccess() { return false; }

        @Override
        public boolean isFailure() { return true; }

        @Override
        public T get() { throw new NoSuchElementException(); }

        @Override
        public T getOrElse(T fallback) { return fallback; }

        @Override
        public T getOrElse(Supplier<T> fallback) { return fallback.get(); }

        @Override
        public Throwable getFailure() { return thrown; }

        @Override
        public Try<T> orElse(Try<T> fallback) { return fallback; }

        @Override
        public Try<T> orElse(Supplier<Try<T>> fallback) { return fallback.get(); }

        @Override
        public Try<T> recover(Function<Throwable, Optional<T>> f) {
            Optional<T> recovered = f.apply(thrown);
            if(recovered.isPresent()) {
                return success(recovered.get());
            } else {
                return this;
            }
        }

        @Override
        public Optional<T> toOptional() { return Optional.empty(); }

        @Override
        public void ifSuccess(Consumer<? super T> f) { /* do nothing */ }

        @Override
        public void ifFailure(Consumer<? super Throwable> f) { f.accept(thrown); }

        @Override
        public <U> Try<U> map(Function<? super T, ? extends U> f) {
            return failure(thrown);
        }

        @Override
        public <U> Try<U> flatMap(Function<? super T, Try<U>> f) {
            return failure(thrown);
        }

        @Override
        public int hashCode() {
            return Objects.hash(thrown);
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof Failure) {
                Failure<?> that = (Failure<?>) other;
                return Objects.equals(this.thrown, that.thrown);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "failure(" + thrown + ")";
        }
    }

    public static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    public static <T> Try<T> failure(Throwable thrown) {
        return new Failure<>(thrown);
    }

    public static <T> Try<T> tryGet(Callable<? extends T> f) {
        try {
            return success(f.call());
        } catch(Throwable t) {
            return failure(t);
        }
    }

    // private constructor to prevent subclassing
    private Try() {}

    public abstract boolean isSuccess();
    public abstract boolean isFailure();
    public abstract T get();
    public abstract T getOrElse(T fallback);
    public abstract T getOrElse(Supplier<T> fallback);
    public abstract Throwable getFailure();
    public abstract Try<T> orElse(Try<T> fallback);
    public abstract Try<T> orElse(Supplier<Try<T>> fallback);
    public abstract Try<T> recover(Function<Throwable, Optional<T>> f);
    public abstract Optional<T> toOptional();
    public abstract void ifSuccess(Consumer<? super T> f);
    public abstract void ifFailure(Consumer<? super Throwable> f);
    public abstract <U> Try<U> map(Function<? super T, ? extends U> f);
    public abstract <U> Try<U> flatMap(Function<? super T, Try<U>> f);
}
