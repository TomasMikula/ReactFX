package org.reactfx;

import java.util.function.Consumer;

class HookStream<T> extends EventStreamBase<T> {
    private final EventStream<T> source;
    private final Consumer<? super T> sideEffect;
    private boolean sideEffectInProgress = false;

    public HookStream(EventStream<T> source, Consumer<? super T> sideEffect) {
        this.source = source;
        this.sideEffect = sideEffect;
    }

    @Override
    protected Subscription observeInputs() {
        return source.subscribe(t -> {
            if(sideEffectInProgress) {
                throw new IllegalStateException("Side effect is not allowed to cause recursive event emission");
            }

            sideEffectInProgress = true;
            try {
                sideEffect.accept(t);
            } finally {
                sideEffectInProgress = false;
            }

            emit(t);
        });
    }
}