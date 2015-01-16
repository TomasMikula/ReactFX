package org.reactfx;


class EmitOnStream<T> extends EventStreamBase<T> {
    private final EventStream<T> source;
    private final EventStream<?> impulse;

    private boolean hasValue = false;
    private T value = null;

    public EmitOnStream(EventStream<T> source, EventStream<?> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription observeInputs() {
        Subscription s1 = source.subscribe(v -> {
            hasValue = true;
            value = v;
        });

        Subscription s2 = impulse.subscribe(i -> {
            if(hasValue) {
                T val = value;
                hasValue = false;
                value = null;
                emit(val);
            }
        });

        return s1.and(s2);
    }
}