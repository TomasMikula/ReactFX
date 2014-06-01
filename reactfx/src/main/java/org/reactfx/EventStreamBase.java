package org.reactfx;

import java.util.function.Consumer;

import org.reactfx.util.ListHelper;

/**
 *
 * @param <S> type of the subscriber
 */
public abstract class EventStreamBase<S> {

    private ListHelper<S> subscribers = null;

    protected final void forEachSubscriber(Consumer<S> action) {
        ListHelper.forEach(subscribers, action);
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

    public final Subscription subscribe(S subscriber) {
        subscribers = ListHelper.add(subscribers, subscriber);
        newSubscriber(subscriber);
        if(ListHelper.size(subscribers) == 1) {
            firstSubscriber();
        }
        return () -> unsubscribe(subscriber);
    }

    private void unsubscribe(S subscriber) {
        subscribers = ListHelper.remove(subscribers, subscriber);
        if(ListHelper.isEmpty(subscribers)) {
            noSubscribers();
        }
    }
}
