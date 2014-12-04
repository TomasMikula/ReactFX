package org.reactfx;


public final class ConnectableEventSource<T>
extends ConnectableEventSourceBase<Subscriber<? super T>>
implements ConnectableEventStream<T>, ConnectableEventSink<T> {

    @Override
    public void push(T value) {
        notifyObservers(s -> s.onEvent(value));
    }

    @Override
    public Subscription connectTo(EventStream<? extends T> source) {
        return newInput(source, src -> subscribeTo(src, this::push));
    }
}
