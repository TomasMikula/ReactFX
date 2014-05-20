package org.reactfx;

import java.util.function.Consumer;

public abstract class EventStreamBase<T> implements EventStream<T> {

    private ListHelper<Consumer<? super T>> subscribers = null;

    protected void emit(T value) {
        ListHelper.forEach(subscribers, s -> s.accept(value));
    }

    /**
     * Called when the number of subscribers goes from 0 to 1.
     * Overriding this method is a convenient way for subclasses
     * to handle this event.
     *
     * <p>This method is called after the {@link #newSubscriber(Consumer)}
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
    protected void newSubscriber(Consumer<? super T> consumer) {
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

    @Override
    public Subscription subscribe(Consumer<? super T> consumer) {
        subscribers = ListHelper.add(subscribers, consumer);
        newSubscriber(consumer);
        if(ListHelper.size(subscribers) == 1) {
            firstSubscriber();
        }
        return () -> unsubscribe(consumer);
    }

    private void unsubscribe(Consumer<? super T> consumer) {
        subscribers = ListHelper.remove(subscribers, consumer);
        if(ListHelper.isEmpty(subscribers)) {
            noSubscribers();
        }
    }
}
