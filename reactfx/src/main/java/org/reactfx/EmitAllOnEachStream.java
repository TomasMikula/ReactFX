package org.reactfx;


class EmitBothOnEachStream<A, I> extends LazilyBoundBiStream<A, I> {
    private final EventStream<A> source;
    private final EventStream<I> impulse;

    private boolean hasValue = false;
    private A a = null;

    public EmitBothOnEachStream(EventStream<A> source, EventStream<I> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = subscribeTo(source, a -> {
            hasValue = true;
            this.a = a;
        });

        Subscription s2 = subscribeTo(impulse, i -> {
            if(hasValue) {
                emit(a, i);
            }
        });

        return s1.and(s2);
    }
}

class EmitAll3OnEachStream<A, B, I> extends LazilyBoundTriStream<A, B, I> {
    private final BiEventStream<A, B> source;
    private final EventStream<I> impulse;

    private boolean hasValue = false;
    private A a = null;
    private B b = null;

    public EmitAll3OnEachStream(
            BiEventStream<A, B> source,
            EventStream<I> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = subscribeTo(source, (a, b) -> {
            hasValue = true;
            this.a = a;
            this.b = b;
        });

        Subscription s2 = subscribeTo(impulse, i -> {
            if(hasValue) {
                emit(a, b, i);
            }
        });

        return s1.and(s2);
    }
}