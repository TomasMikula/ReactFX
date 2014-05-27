package org.reactfx;

import javafx.beans.value.ObservableBooleanValue;

/**
 * An event stream that indicates whether there is a pending event that can
 * be expected to be emitted in the future.
 *
 * <p>A stream may indicate a pending event while it is awaiting a timer or
 * completion of an asynchronous task.
 *
 * @param <T>
 */
public interface AwaitingEventStream<T> extends EventStream<T> {
    /**
     * Indicates whether there is a pending event that will be emitted by this
     * stream in the (near) future. This may mean that an event has occurred
     * that causes this stream to emit an event with some delay, e.g. waiting
     * for a timer or completion of an asynchronous task.
     */
    ObservableBooleanValue pendingProperty();
    boolean isPending();
}
