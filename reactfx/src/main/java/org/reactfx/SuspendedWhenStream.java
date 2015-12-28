package org.reactfx;

import javafx.beans.value.ObservableValue;

class SuspendedWhenStream<T> extends EventStreamBase<T> {
    private final SuspendableEventStream<T> source;
    private final ObservableValue<Boolean> condition;

    public SuspendedWhenStream(
            SuspendableEventStream<T> source,
            ObservableValue<Boolean> condition) {
        this.source = source;
        this.condition = condition;
    }

    @Override
    protected Subscription observeInputs() {
        Subscription s1 = source.suspendWhen(condition);
        Subscription s2 = source.subscribe(this::emit);
        return s1.and(s2);
    }
}
