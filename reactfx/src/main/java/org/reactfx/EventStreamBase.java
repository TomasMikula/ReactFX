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
    private boolean reporting = false;
    private boolean unnotifiedSubscribers = false;

    protected final int getSubscriberCount() {
        return ListHelper.size(subscribers);
    }

    protected final void tryRun(Runnable action) {
        try {
            action.run();
        } catch(Throwable t) {
            reportError(t);
        }
    }

    protected final void forEachSubscriber(Consumer<S> action) {
        if(unnotifiedSubscribers) {
            try {
                throw new IllegalStateException("Cannot recursively emit"
                        + " before all subscribers were notified of the"
                        + " previous event");
            } catch(IllegalStateException e) {
                e.printStackTrace();
                reportError(e);
            }
            return;
        }

        if(ListHelper.size(subscribers) > 1) {
            // prevent recursion when there are 2 or more subscribers
            unnotifiedSubscribers = true;
        }
        ListHelper.forEach(subscribers, s -> {
            try {
                action.accept(s);
            } catch(Throwable t) {
                reportError(t);
            }
        });
        unnotifiedSubscribers = false;
    }

    /**
     * Notifies all registered error handlers of the given error.
     */
    protected final void reportError(Throwable thrown) {
        if(reporting) {
            // Error reporting caused another error in the same stream.
            // Don't notify handlers again (likely to cause an error again).
            // Just print the stack trace.
            thrown.printStackTrace();
        } else {
            reporting = true;
            ListHelper.forEach(monitors, m -> {
                try {
                    m.accept(thrown);
                } catch(Throwable another) { // error handler threw an exception
                    another.printStackTrace();
                }
            });
            reporting = false;
        }
    }

    /**
     * Called when the number of subscribers goes from 0 to 1.
     * Overriding this method is a convenient way for subclasses
     * to handle this event.
     *
     * <p>This method is called <em>before</em> the
     * {@link #newSubscriber(Object)} method.</p>
     */
    protected void firstSubscriber() {
        // default implementation is empty
    }

    /**
     * Called for each new subscriber.
     * Overriding this method is a convenient way for subclasses
     * to handle this event, for example to publish some initial events.
     *
     * <p>This method is called <em>after</em> the
     * {@link #firstSubscriber()} method.</p>
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
     * Subscribes to the given event stream by the given subscriber and also
     * forwards errors reported by the given stream to this stream. This is
     * equivalent to {@code stream.subscribe(subscriber, this::reportError)}.
     * @return subscription used to unsubscribe {@code subscriber} from
     * {@code stream} and stop forwarding the errors.
     */
    protected final <T> Subscription subscribeTo(
            EventStream<T> stream,
            Consumer<? super T> subscriber) {
        return stream.subscribe(subscriber, this::reportError);
    }

    /**
     * Subscribes to the given event stream by the given subscriber and also
     * forwards errors reported by the given stream to this stream. This is
     * equivalent to {@code stream.subscribe(subscriber, this::reportError)}.
     * @return subscription used to unsubscribe {@code subscriber} from
     * {@code stream} and stop forwarding the errors.
     */
    protected final <A, B> Subscription subscribeToBi(
            BiEventStream<A, B> stream,
            BiConsumer<? super A, ? super B> subscriber) {
        return stream.subscribe(subscriber, this::reportError);
    }

    /**
     * Subscribes to the given event stream by the given subscriber and also
     * forwards errors reported by the given stream to this stream. This is
     * equivalent to {@code stream.subscribe(subscriber, this::reportError)}.
     * @return subscription used to unsubscribe {@code subscriber} from
     * {@code stream} and stop forwarding the errors.
     */
    protected final <A, B, C> Subscription subscribeToTri(
            TriEventStream<A, B, C> stream,
            TriConsumer<? super A, ? super B, ? super C> subscriber) {
        return stream.subscribe(subscriber, this::reportError);
    }

    public final Subscription subscribe(
            S subscriber,
            Consumer<? super Throwable> onError) {
        Subscription s1 = monitor(onError);

        subscribers = ListHelper.add(subscribers, subscriber);
        if(ListHelper.size(subscribers) == 1) {
            firstSubscriber();
        }
        newSubscriber(subscriber);

        return s1.and(() -> unsubscribe(subscriber));
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
