package org.reactfx;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.TriConsumer;

/**
 * Base class for an event stream that has one or more sources (most commonly
 * event streams, but not necessarily) to which it is subscribed only when it
 * itself has at least one subscriber.
 *
 * @param <S> type of the subscriber
 */
public abstract class LazilyBoundStreamBase<S> extends EventStreamBase<S> {
    private Subscription subscription = null;

    protected abstract Subscription subscribeToInputs();

    @Override
    protected final void firstSubscriber() {
        try {
            subscription = subscribeToInputs();
        } catch(Throwable t) {
            reportError(t);
        }
    }

    @Override
    protected final void noSubscribers() {
        try {
            subscription.unsubscribe();
            subscription = null;
        } catch(Throwable t) {
            reportError(t);
        }
    }

    protected final boolean isBound() {
        return subscription != null;
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
}