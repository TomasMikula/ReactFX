package org.reactfx;

import java.util.Optional;
import java.util.function.Function;

class FlatMapStream<T, U> extends LazilyBoundStream<U> {
    private final EventStream<T> source;
    private final Function<? super T, ? extends EventStream<U>> mapper;

    private Subscription mappedSubscription = Subscription.EMPTY;

    public FlatMapStream(
            EventStream<T> src,
            Function<? super T, ? extends EventStream<U>> f) {
        this.source = src;
        this.mapper = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s = subscribeTo(source, t -> {
            mappedSubscription.unsubscribe();
            mappedSubscription = mapper.apply(t).subscribe(u -> emit(u));
        });
        return () -> {
            s.unsubscribe();
            mappedSubscription.unsubscribe();
            mappedSubscription = Subscription.EMPTY;
        };
    }
}

class FlatMapOptStream<T, U> extends LazilyBoundStream<U> {
    private final EventStream<T> source;
    private final Function<? super T, Optional<U>> mapper;

    public FlatMapOptStream(
            EventStream<T> src,
            Function<? super T, Optional<U>> f) {
        this.source = src;
        this.mapper = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(
                source,
                t -> mapper.apply(t).ifPresent(this::emit));
    }
}
