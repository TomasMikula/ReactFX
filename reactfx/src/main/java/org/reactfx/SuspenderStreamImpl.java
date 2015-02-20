package org.reactfx;

class SuspenderStreamImpl<T, S extends Suspendable>
extends EventStreamBase<T> implements Suspender<S>, SuspenderStream<T, S> {
    private final EventStream<T> source;
    private final S suspendable;

    public SuspenderStreamImpl(EventStream<T> source, S suspendable) {
        this.source = source;
        this.suspendable = suspendable;
    }

    @Override
    protected Subscription observeInputs() {
        return source.subscribe(evt -> {
            try(Guard g = suspendable.suspend()) {
                emit(evt);
            }
        });
    }

    @Override
    public S getSuspendable() {
        return suspendable;
    }
}