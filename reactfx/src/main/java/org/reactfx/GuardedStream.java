package org.reactfx;

class GuardedStream<T> extends EventStreamBase<T> {
    private final EventStream<T> source;
    private final Suspendable suspendable;

    public GuardedStream(EventStream<T> source, Suspendable suspendable) {
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
}