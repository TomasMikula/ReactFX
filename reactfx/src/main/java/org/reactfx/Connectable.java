package org.reactfx;

/**
 * Interface for objects that can be (lazily) connected to event streams.
 * The semantics of a connection is left to the implementations of this
 * interface.
 */
public interface Connectable<T> {

    /**
     * Connects this connectable object to {@code source} event stream.
     * Implementations of this method should subscribe to {@code source}
     * lazily, i.e. only subscribe to {@code source} when necessary, e.g.
     * when the connectable object itself is being observed (e.g. itself
     * has at least one subscriber).
     * <p>A {@code Connectable} may be connected to multiple sources at
     * the same time.
     * @param source event stream to (lazily) connect to.
     * @return subscription that can be used to disconnect this connectable
     * object from {@code source}.
     */
    Subscription connectTo(EventStream<? extends T> source);
}
