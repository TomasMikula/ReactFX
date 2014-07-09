package org.reactfx;

import java.util.function.BinaryOperator;

class ReducibleEventStream<T> extends SuspendableEventStreamBase<T> {
    private final BinaryOperator<T> reduction;
    private boolean hasValue = false;
    private T value = null;

    public ReducibleEventStream(
            EventStream<T> source,
            BinaryOperator<T> reduction) {
        super(source);
        this.reduction = reduction;
    }

    @Override
    protected void handleEventWhenSuspended(T event) {
        if(hasValue) {
            value = reduction.apply(value, event);
        } else {
            value = event;
            hasValue = true;
        }
    }

    @Override
    protected void onResume() {
        if(hasValue) {
            hasValue = false;
            T toEmit = value;
            value = null;
            emit(toEmit);
        }
    }
}
