package org.reactfx;

import java.util.ArrayList;
import java.util.List;


class PausableEventStream<T> extends SuspendableEventStreamBase<T> {

    private List<T> pendingEvents = null;

    PausableEventStream(EventStream<T> source) {
        super(source);
    }

    @Override
    protected void handleEventWhenSuspended(T event) {
        if(pendingEvents == null) {
            pendingEvents = new ArrayList<>();
        }
        pendingEvents.add(event);
    }

    @Override
    protected void onResume() {
        if(pendingEvents != null) {
            List<T> toEmit = pendingEvents;
            pendingEvents = null;
            for(T t: toEmit) {
                emit(t);
            }
        }
    }
}