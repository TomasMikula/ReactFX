package org.reactfx;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.TriConsumer;

class SideEffectStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final Consumer<? super T> sideEffect;

    public SideEffectStream(EventStream<T> source, Consumer<? super T> sideEffect) {
        this.source = source;
        this.sideEffect = sideEffect;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return source.subscribe(t -> {
            sideEffect.accept(t);
            emit(t);
        });
    }
}

class SideEffectBiStream<A, B> extends LazilyBoundBiStream<A, B> {
    private final BiEventStream<A, B> source;
    private final BiConsumer<? super A, ? super B> sideEffect;

    public SideEffectBiStream(BiEventStream<A, B> source, BiConsumer<? super A, ? super B> sideEffect) {
        this.source = source;
        this.sideEffect = sideEffect;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return source.subscribe((a, b) -> {
            sideEffect.accept(a, b);
            emit(a, b);
        });
    }
}


class SideEffectTriStream<A, B, C> extends LazilyBoundTriStream<A, B, C> {
    private final TriEventStream<A, B, C> source;
    private final TriConsumer<? super A, ? super B, ? super C> sideEffect;

    public SideEffectTriStream(TriEventStream<A, B, C> source, TriConsumer<? super A, ? super B, ? super C> sideEffect) {
        this.source = source;
        this.sideEffect = sideEffect;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return source.subscribe((a, b, c) -> {
            sideEffect.accept(a, b, c);
            emit(a, b, c);
        });
    }
}