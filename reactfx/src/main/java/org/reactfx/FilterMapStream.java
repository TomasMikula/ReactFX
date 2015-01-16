package org.reactfx;

import java.util.function.Function;
import java.util.function.Predicate;

class FilterMapStream<T, U> extends EventStreamBase<U> {
    private final EventStream<T> source;
    private final Predicate<? super T> predicate;
    private final Function<? super T, ? extends U> f;

    public FilterMapStream(
            EventStream<T> source,
            Predicate<? super T> predicate,
            Function<? super T, ? extends U> f) {
        this.source = source;
        this.predicate = predicate;
        this.f = f;
    }

    @Override
    protected Subscription observeInputs() {
        return source.subscribe(value -> {
            if(predicate.test(value)) {
                emit(f.apply(value));
            }
        });
    }
}