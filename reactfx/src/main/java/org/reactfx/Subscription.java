package org.reactfx;

public interface Subscription {
    void unsubscribe();

    static final Subscription EMPTY = () -> {};

    static Subscription multi(Subscription... subscriptions) {
        return () -> {
            for(Subscription s: subscriptions) {
                s.unsubscribe();
            }
        };
    }
}
