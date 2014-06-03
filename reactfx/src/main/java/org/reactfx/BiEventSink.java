package org.reactfx;

import org.reactfx.util.Tuple2;

@FunctionalInterface
public interface BiEventSink<A, B> extends EventSink<Tuple2<A, B>> {
    void push(A a, B b);

    @Override
    default void push(Tuple2<A, B> t) {
        push(t._1, t._2);
    }

    default Subscription feedFrom2(BiEventStream<? extends A, ? extends B> source) {
        return source.subscribe(this::push);
    }
}
