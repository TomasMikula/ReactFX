package org.reactfx;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.reactfx.util.TriPredicate;

class FilterStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final Predicate<? super T> predicate;

    public FilterStream(
            EventStream<T> source,
            Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return source.subscribe(t -> {
            if(predicate.test(t)) {
                emit(t);
            }
        });
    }
}

class FilterBiStream<A, B> extends LazilyBoundBiStream<A, B> {
    private final BiEventStream<A, B> source;
    private final BiPredicate<? super A, ? super B> predicate;

    public FilterBiStream(
            BiEventStream<A, B> source,
            BiPredicate<? super A, ? super B> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return source.subscribe((a, b) -> {
            if(predicate.test(a, b)) {
                emit(a, b);
            }
        });
    }
}

class FilterTriStream<A, B, C> extends LazilyBoundTriStream<A, B, C> {
    private final TriEventStream<A, B, C> source;
    private final TriPredicate<? super A, ? super B, ? super C> predicate;

    public FilterTriStream(
            TriEventStream<A, B, C> source,
            TriPredicate<? super A, ? super B, ? super C> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return source.subscribe((a, b, c) -> {
            if(predicate.test(a, b, c)) {
                emit(a, b, c);
            }
        });
    }
}