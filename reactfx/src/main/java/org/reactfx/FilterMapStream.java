package org.reactfx;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactfx.util.TriFunction;
import org.reactfx.util.TriPredicate;

class FilterMapStream<T, U> extends LazilyBoundStream<U> {
    private final EventStream<T> source;
    private final Predicate<? super T> predicate;
    private final Function<? super T, ? extends U> f;

    public FilterMapStream(
            EventStream<T> source,
            Predicate<? super T> predicate,
            Function<? super T, ? extends U> f) {
        this.source = source;
        this.predicate = predicate;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(source, value -> {
            if(predicate.test(value)) {
                emit(f.apply(value));
            }
        });
    }
}

class FilterMapBiStream<A, B, U> extends LazilyBoundStream<U> {
    private final BiEventStream<A, B> source;
    private final BiPredicate<? super A, ? super B> predicate;
    private final BiFunction<? super A, ? super B, ? extends U> f;

    public FilterMapBiStream(
            BiEventStream<A, B> source,
            BiPredicate<? super A, ? super B> predicate,
            BiFunction<? super A, ? super B, ? extends U> f) {
        this.source = source;
        this.predicate = predicate;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(source, (a, b) -> {
            if(predicate.test(a, b)) {
                emit(f.apply(a, b));
            }
        });
    }
}

class FilterMapTriStream<A, B, C, U> extends LazilyBoundStream<U> {
    private final TriEventStream<A, B, C> source;
    private final TriPredicate<? super A, ? super B, ? super C> predicate;
    private final TriFunction<? super A, ? super B, ? super C, ? extends U> f;

    public FilterMapTriStream(
            TriEventStream<A, B, C> source,
            TriPredicate<? super A, ? super B, ? super C> predicate,
            TriFunction<? super A, ? super B, ? super C, ? extends U> f) {
        this.source = source;
        this.predicate = predicate;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(source, (a, b, c) -> {
            if(predicate.test(a, b, c)) {
                emit(f.apply(a, b, c));
            }
        });
    }
}
