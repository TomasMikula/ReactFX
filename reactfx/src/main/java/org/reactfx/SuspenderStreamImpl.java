package org.reactfx;

import java.util.function.Consumer;

class SuspenderStreamImpl<T, S extends Suspendable>
extends SuspenderBase<Consumer<? super T>, T, S>
implements SuspenderStream<T, S>, ProperEventStream<T> {
    private final EventStream<T> source;

    public SuspenderStreamImpl(EventStream<T> source, S suspendable) {
        super(suspendable);
        this.source = source;
    }

    @Override
    protected Subscription observeInputs() {
        return source.subscribe(this::notifyObserversWhileSuspended);
    }
}