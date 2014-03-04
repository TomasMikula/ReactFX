package reactfx;

/**
 * Event stream that has one or more sources (most commonly event streams,
 * but not necessarily) to which it is subscribed only when it itself has
 * at least one subscriber.
 *
 * @param <T> type of events emitted by this event stream.
 */
public abstract class LazilyBoundStream<T> extends EventStreamBase<T> {
    private Subscription subscription = null;

    protected abstract Subscription subscribeToInputs();

    @Override
    protected final void firstSubscriber() {
        subscription = subscribeToInputs();
    }

    @Override
    protected final void noSubscribers() {
        subscription.unsubscribe();
        subscription = null;
    }
}