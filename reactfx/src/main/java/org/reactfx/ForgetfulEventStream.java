package org.reactfx;

class ForgetfulEventStream<T> extends SuspendableEventStreamBase<T> {
    private boolean hasValue = false;
    private T value = null;

    ForgetfulEventStream(EventStream<T> source) {
        super(source);
    }

    @Override
    protected void handleEventWhenSuspended(T event) {
        value = event;
        hasValue = true;
    }

    @Override
    protected void onResume() {
        if(hasValue) {
            T toEmit = value;
            reset();
            emit(toEmit);
        }
    }

    @Override
    protected void reset() {
        hasValue = false;
        value = null;
    }
}