package org.reactfx;

import org.reactfx.util.Tuple3;

@FunctionalInterface
public interface TriEventSink<A, B, C> extends EventSink<Tuple3<A, B, C>> {
    void push(A a, B b, C c);

    @Override
    default void push(Tuple3<A, B, C> t) {
        push(t._1, t._2, t._3);
    }

    default Subscription feedFrom3(TriEventStream<? extends A, ? extends B, ? extends C> source) {
        return source.subscribe((a, b, c) -> push(a, b, c));
    }
}
