package org.reactfx;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.ListHelper;
import org.reactfx.util.TriConsumer;

/**
 *
 * @param <S> type of the subscriber
 */
public abstract class EventStreamBase<S> {

    private ListHelper<S> subscribers = null;
    private ListHelper<Consumer<? super Throwable>> monitors = null;

    protected final void forEachSubscriber(Consumer<S> action) {
        ListHelper.forEach(subscribers, s -> {
            try {
                action.accept(s);
            } catch(Throwable t) {
                reportError(t);
            }
        });
    }

    protected final void reportError(Throwable thrown) {
        ListHelper.forEach(monitors, m -> {
            try {
                m.accept(thrown);
            } catch(Throwable another) {
                ListHelper.forEach(monitors, n -> {
                    try {
                        n.accept(another);
                    } catch(Throwable t) {
                        // ignore
                    }
                });
            }
        });
    }

    /**
     * Called when the number of subscribers goes from 0 to 1.
     * Overriding this method is a convenient way for subclasses
     * to handle this event.
     *
     * <p>This method is called after the {@link #newSubscriber(Object)}
     * method.</p>
     */
    protected void firstSubscriber() {
        // default implementation is empty
    }

    /**
     * Called for each new subscriber.
     * Overriding this method is a convenient way for subclasses
     * to handle this event, for example to publish some initial events.
     */
    protected void newSubscriber(S subscriber) {
        // default implementation is empty
    }

    /**
     * Called when the number of subscribers goes down to 0.
     * Overriding this method is a convenient way for subclasses
     * to handle this event.
     */
    protected void noSubscribers() {
        // default implementation is empty
    }

    /**
     * Forwards errors reported by the given stream to this stream.
     * @return subscription that can be used to stop forwarding the errors.
     */
    protected final Subscription oversee(EventStream<?> stream) {
        return stream.monitor(this::reportError);
    }

    /**
     * Subscribes to the given event stream by the given subscriber and also
     * forwards errors reported by the given stream to this stream.
     * @return subscription used to unsubscribe {@code subscriber} from
     * {@code stream} and stop forwarding the errors.
     */
    protected final <T> Subscription subscribeTo(
            EventStream<T> stream,
            Consumer<? super T> subscriber) {
        return oversee(stream).and(stream.subscribe(subscriber));
    }

    /**
     * Subscribes to the given event stream by the given subscriber and also
     * forwards errors reported by the given stream to this stream.
     * @return subscription used to unsubscribe {@code subscriber} from
     * {@code stream} and stop forwarding the errors.
     */
    protected final <A, B> Subscription subscribeTo(
            BiEventStream<A, B> stream,
            BiConsumer<? super A, ? super B> subscriber) {
        return oversee(stream).and(stream.subscribe(subscriber));
    }

    /**
     * Subscribes to the given event stream by the given subscriber and also
     * forwards errors reported by the given stream to this stream.
     * @return subscription used to unsubscribe {@code subscriber} from
     * {@code stream} and stop forwarding the errors.
     */
    protected final <A, B, C> Subscription subscribeTo(
            TriEventStream<A, B, C> stream,
            TriConsumer<? super A, ? super B, ? super C> subscriber) {
        return oversee(stream).and(stream.subscribe(subscriber));
    }

    public final Subscription subscribe(S subscriber) {
        subscribers = ListHelper.add(subscribers, subscriber);
        newSubscriber(subscriber);
        if(ListHelper.size(subscribers) == 1) {
            firstSubscriber();
        }
        return () -> unsubscribe(subscriber);
    }

    public final Subscription monitor(Consumer<? super Throwable> monitor) {
        monitors = ListHelper.add(monitors, monitor);
        return () -> monitors = ListHelper.remove(monitors, monitor);
    }

    private void unsubscribe(S subscriber) {
        subscribers = ListHelper.remove(subscribers, subscriber);
        if(ListHelper.isEmpty(subscribers)) {
            noSubscribers();
        }
    }
}
