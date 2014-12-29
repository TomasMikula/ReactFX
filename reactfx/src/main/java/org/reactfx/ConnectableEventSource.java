package org.reactfx;

import org.reactfx.util.MapHelper;


public final class ConnectableEventSource<T>
extends EventStreamBase<T>
implements ConnectableEventStream<T>, ConnectableEventSink<T> {

    private MapHelper<EventStream<? extends T>, Subscription> subscriptions = null;

    @Override
    public final void push(T value) {
        emit(value);
    }

    @Override
    public Subscription connectTo(EventStream<? extends T> input) {

        if(MapHelper.containsKey(subscriptions, input)) {
            throw new IllegalStateException("Already connected to event stream " + input);
        }

        Subscription sub = isBound() ? subscribeToInput(input) : null;
        subscriptions = MapHelper.put(subscriptions, input, sub);

        return () -> {
            Subscription s = MapHelper.get(subscriptions, input);
            subscriptions = MapHelper.remove(subscriptions, input);
            if(s != null) {
                s.unsubscribe();
            }
        };
    }

    @Override
    protected final Subscription bindToInputs() {
        MapHelper.replaceAll(subscriptions, (input, sub) -> subscribeToInput(input));
        return () -> MapHelper.replaceAll(subscriptions, (input, sub) -> {
            sub.unsubscribe();
            return null;
        });
    }

    private final Subscription subscribeToInput(EventStream<? extends T> input) {
        return input.subscribe(this::push);
    }
}
