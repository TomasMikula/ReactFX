package org.reactfx;

@FunctionalInterface
public interface EventSink<T> {
    void push(T value);

    /**
     * Starts pushing all events emitted by {@code source} to this event sink.
     * <p>An event sink can be fed from multiple sources at the same time.
     * <p>{@code sink.feedFrom(stream)} is equivalent to
     * {@code stream.feedTo(sink)}
     * @param source event stream whose events will be pushed to this event sink
     * @return subscription that can be used to stop delivering {@code source}'s
     * events to this event sink.
     * @see EventStream#feedTo(EventSink)
     */
    default Subscription feedFrom(EventStream<? extends T> source) {
        return source.subscribe(this::push);
    }
}
