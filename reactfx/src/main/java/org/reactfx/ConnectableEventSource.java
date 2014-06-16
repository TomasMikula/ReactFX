package org.reactfx;

import java.util.function.Consumer;

public final class ConnectableEventSource<T>
extends ConnectableEventSourceBase<Consumer<? super T>>
implements ConnectableEventStream<T>, ConnectableEventSink<T> {

    @Override
    public void push(T value) {
        forEachSubscriber(s -> s.accept(value));
    }

    @Override
    public Subscription connectTo(EventStream<? extends T> source) {
        return newInput(source, src -> subscribeTo(src, this::push));
    }
}
