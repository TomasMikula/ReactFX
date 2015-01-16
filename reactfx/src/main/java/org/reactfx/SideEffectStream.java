package org.reactfx;

import java.util.function.Consumer;

class SideEffectStream<T> extends EventStreamBase<T> {
    private final EventStream<T> source;
    private final Consumer<? super T> sideEffect;
    private boolean sideEffectInProgress = false;
    private boolean sideEffectCausedRecursion = false;

    public SideEffectStream(EventStream<T> source, Consumer<? super T> sideEffect) {
        this.source = source;
        this.sideEffect = sideEffect;
    }

    @Override
    protected Subscription observeInputs() {
        return source.subscribe(t -> {
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