package org.reactfx;


class SuppressibleEventStream<T> extends SuspendableEventStreamBase<T> {

    SuppressibleEventStream(EventStream<T> source) {
        super(source);
    }

    @Override
    protected void handleEventWhenSuspended(T event) {
        // do nothing, ignore the event
    }

    @Override
    protected void reset() {
        // do nothing, no state to reset
    }
}