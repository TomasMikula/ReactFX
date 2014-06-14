package org.reactfx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.collections.ObservableSet;

@FunctionalInterface
public interface Subscription {
    void unsubscribe();

    /**
     * Returns a new aggregate subscription whose {@link #unsubscribe()}
     * method calls {@code unsubscribe()} on both this subscription and
     * {@code other} subscription.
     */
    default Subscription and(Subscription other) {
        return new BiSubscription(this, other);
    }

    static final Subscription EMPTY = () -> {};

    /**
     * Returns a new aggregate subscription whose {@link #unsubscribe()}
     * method calls {@code unsubscribe()} on all arguments to this method.
     */
    static Subscription multi(Subscription... subs) {
        switch(subs.length) {
            case 0: return EMPTY;
            case 1: return subs[0];
            case 2: return new BiSubscription(subs[0], subs[1]);
            default: return new MultiSubscription(subs);
        }
    }

    /**
     * Subscribes to all elements by the given function and returns an aggregate
     * subscription that can be used to cancel all element subscriptions.
     */
    @SafeVarargs
    static <T> Subscription multi(
            Function<? super T, ? extends Subscription> f,
            T... elems) {
        return multi(Stream.of(elems).map(f)
                .<Subscription>toArray(n -> new Subscription[n]));
    }

    /**
     * Subscribes to all elements of the given collection by the given function
     * and returns an aggregate subscription that can be used to cancel all
     * element subscriptions.
     */
    static <T> Subscription multi(
            Function<? super T, ? extends Subscription> f,
            Collection<T> elems) {
        return multi(elems.stream().map(f)
                .<Subscription>toArray(n -> new Subscription[n]));
    }

    /**
     * Dynamically subscribes to all elements of the given observable set.
     * When an element is added to the set, it is automatically subscribed to.
     * When an element is removed from the set, it is automatically unsubscribed
     * from.
     * @param elems observable set of elements that will be subscribed to
     * @param f function to subscribe to an element of the set.
     * @return An aggregate subscription that tracks elementary subscriptions.
     * When the returned subscription is unsubscribed, all elementary
     * subscriptions are unsubscribed as well, and no new elementary
     * subscriptions will be created.
     */
    static <T> Subscription dynamic(
            ObservableSet<T> elems,
            Function<? super T, ? extends Subscription> f) {

        Map<T, Subscription> elemSubs = new HashMap<>();
        elems.forEach(t -> elemSubs.put(t, f.apply(t)));

        Subscription setSub = EventStreams.changesOf(elems).subscribe(ch -> {
            if(ch.wasRemoved()) {
                Subscription sub = elemSubs.remove(ch.getElementRemoved());
                assert sub != null;
                sub.unsubscribe();
            }
            if(ch.wasAdded()) {
                T elem = ch.getElementAdded();
                assert !elemSubs.containsKey(elem);
                elemSubs.put(elem, f.apply(elem));
            }
        });

        return () -> {
            setSub.unsubscribe();
            elemSubs.forEach((t, sub) -> sub.unsubscribe());
        };
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