package org.reactfx;

import javafx.beans.value.ObservableValue;

class SuspendedWhenStream<T> extends EventStreamBase<T> {
    private final SuspendableEventStream<T> source;
    private final ObservableValue<Boolean> condition;

    private Guard suspensionGuard = null;

    public SuspendedWhenStream(
            SuspendableEventStream<T> source,
            ObservableValue<Boolean> condition) {
        this.source = source;
        this.condition = condition;
    }

    @Override
    protected Subscription observeInputs() {
        Subscription s1 = EventStreams.valuesOf(condition)
                .subscribe(this::suspendSource);
        Subscription s2 = source.subscribe(this::emit);
        return s1.and(s2).and(this::resumeSource);
    }

    private void suspendSource(boolean suspend) {
        if(suspend) {
            suspendSource();
        } else {
            resumeSource();
        }
    }

    private void suspendSource() {
        if(suspensionGuard == null) {
            suspensionGuard = source.suspend();
        }
    }

    private void resumeSource() {
        if(suspensionGuard != null) {
            Guard toClose = suspensionGuard;
            suspensionGuard = null;
            toClose.close();
        }
    }
}
