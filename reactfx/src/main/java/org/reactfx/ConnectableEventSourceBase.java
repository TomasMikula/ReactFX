package org.reactfx;

import java.util.function.Function;

import org.reactfx.util.MapHelper;

class ConnectableEventSourceBase<S extends ErrorHandler>
extends LazilyBoundStreamBase<S> {

    private static final class InputSubscriber<I> {
        private final I input;
        private final Function<? super I, ? extends Subscription> subscribeFn;
        public InputSubscriber(I input, Function<? super I, ? extends Subscription> subscribeFn) {
            this.input = input;
            this.subscribeFn = subscribeFn;
        }
        public Subscription subscribe() {
            return subscribeFn.apply(input);
        }
        @Override
        public boolean equals(Object other) {
            return other instanceof InputSubscriber
                ? input.equals(((InputSubscriber<?>) other).input)
                : false;
        }
        @Override
        public int hashCode() {
            return input.hashCode();
        }
    }

    private MapHelper<InputSubscriber<?>, Subscription> subscriptions = null;

    @Override
    protected final Subscription subscribeToInputs() {
        MapHelper.replaceAll(subscriptions, (input, sub) -> input.subscribe());
        return () -> MapHelper.replaceAll(subscriptions, (input, sub) -> {
            sub.unsubscribe();
            return null;
        });
    }

    protected final <I> Subscription newInput(
            I input,
            Function<? super I, ? extends Subscription> subscriber) {
        InputSubscriber<I> inputSubscriber = new InputSubscriber<>(input, subscriber);

        if(MapHelper.containsKey(subscriptions, inputSubscriber)) {
            throw new IllegalStateException("Already connected to event stream " + input);
        }

        Subscription sub = isBound() ? inputSubscriber.subscribe() : null;
        subscriptions = MapHelper.put(subscriptions, inputSubscriber, sub);

        return () -> {
            Subscription s = MapHelper.get(subscriptions, inputSubscriber);
            subscriptions = MapHelper.remove(subscriptions, inputSubscriber);
            if(s != null) {
                s.unsubscribe();
            }
        };
    }
}
