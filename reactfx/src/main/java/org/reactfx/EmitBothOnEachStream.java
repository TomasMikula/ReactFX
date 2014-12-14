package org.reactfx;

import static org.reactfx.util.Tuples.*;

import org.reactfx.util.Tuple2;

class EmitBothOnEachStream<A, I> extends EventStreamBase<Tuple2<A, I>> {
    private final EventStream<A> source;
    private final EventStream<I> impulse;

    private boolean hasValue = false;
    private A a = null;

    public EmitBothOnEachStream(EventStream<A> source, EventStream<I> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription bindToInputs() {
        Subscription s1 = subscribeTo(source, a -> {
            hasValue = true;
            this.a = a;
        });

        Subscription s2 = subscribeTo(impulse, i -> {
            if(hasValue) {
                emit(t(a, i));
            }
        });

        return s1.and(s2);
    }
}