package org.reactfx;

@FunctionalInterface
public interface Subscription {
    void unsubscribe();

    static final Subscription EMPTY = () -> {};

    static Subscription multi(Subscription... subs) {
        switch(subs.length) {
            case 0: return EMPTY;
            case 1: return subs[0];
            case 2: return new BiSubscription(subs[0], subs[1]);
            default: return new MultiSubscription(subs);
        }
    }
}

class BiSubscription implements Subscription {
    private final Subscription s1;
    private final Subscription s2;

    public BiSubscription(Subscription s1, Subscription s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    @Override
    public void unsubscribe() {
        s1.unsubscribe();
        s2.unsubscribe();
    }
}

class MultiSubscription implements Subscription {
    private final Subscription[] subscriptions;

    public MultiSubscription(Subscription... subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public void unsubscribe() {
        for(Subscription s: subscriptions) {
            s.unsubscribe();
        }
    }
}