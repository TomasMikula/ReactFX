package org.reactfx;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.TriConsumer;

class SideEffectStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final Consumer<? super T> sideEffect;
    private boolean sideEffectInProgress = false;
    private boolean sideEffectCausedRecursion = false;

    public SideEffectStream(EventStream<T> source, Consumer<? super T> sideEffect) {
        this.source = source;
        this.sideEffect = sideEffect;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(source, t -> {
            if(sideEffectInProgress) {
                sideEffectCausedRecursion = true;
                throw new IllegalStateException("Side effect is not allowed to cause recursive event emission");
            }
            sideEffectInProgress = true;
            try {
                sideEffect.accept(t);
            } finally {
                sideEffectInProgress = false;
            }
            if(sideEffectCausedRecursion) {
                // do not emit the event, error has already been reported from a recursive call
                sideEffectCausedRecursion = false;
            } else {
                emit(t);
            }
        });
    }
}

class SideEffectBiStream<A, B> extends LazilyBoundBiStream<A, B> {
    private final BiEventStream<A, B> source;
    private final BiConsumer<? super A, ? super B> sideEffect;
    private boolean sideEffectInProgress = false;
    private boolean sideEffectCausedRecursion = false;

    public SideEffectBiStream(BiEventStream<A, B> source, BiConsumer<? super A, ? super B> sideEffect) {
        this.source = source;
        this.sideEffect = sideEffect;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToBi(source, (a, b) -> {
            if(sideEffectInProgress) {
                sideEffectCausedRecursion = true;
                throw new IllegalStateException("Side effect is not allowed to cause recursive event emission");
            }
            sideEffectInProgress = true;
            try {
                sideEffect.accept(a, b);
            } finally {
                sideEffectInProgress = false;
            }
            if(sideEffectCausedRecursion) {
                // do not emit the event, error has already been reported from a recursive call
                sideEffectCausedRecursion = false;
            } else {
                emit(a, b);
            }
        });
    }
}


class SideEffectTriStream<A, B, C> extends LazilyBoundTriStream<A, B, C> {
    private final TriEventStream<A, B, C> source;
    private final TriConsumer<? super A, ? super B, ? super C> sideEffect;
    private boolean sideEffectInProgress = false;
    private boolean sideEffectCausedRecursion = false;

    public SideEffectTriStream(TriEventStream<A, B, C> source, TriConsumer<? super A, ? super B, ? super C> sideEffect) {
        this.source = source;
        this.sideEffect = sideEffect;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToTri(source, (a, b, c) -> {
            if(sideEffectInProgress) {
                sideEffectCausedRecursion = true;
                throw new IllegalStateException("Side effect is not allowed to cause recursive event emission");
            }
            sideEffectInProgress = true;
            try {
                sideEffect.accept(a, b, c);
            } finally {
                sideEffectInProgress = false;
            }
            if(sideEffectCausedRecursion) {
                // do not emit the event, error has already been reported from a recursive call
                sideEffectCausedRecursion = false;
            } else {
                emit(a, b, c);
            }
        });
    }
}