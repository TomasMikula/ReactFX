package org.reactfx;


class EmitOnStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final EventStream<?> impulse;

    private boolean hasValue = false;
    private T value = null;

    public EmitOnStream(EventStream<T> source, EventStream<?> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = subscribeTo(source, v -> {
            hasValue = true;
            value = v;
        });

        Subscription s2 = subscribeTo(impulse, i -> {
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