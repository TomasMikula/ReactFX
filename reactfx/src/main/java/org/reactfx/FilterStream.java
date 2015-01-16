package org.reactfx;

import java.util.function.Predicate;

class FilterStream<T> extends EventStreamBase<T> {
    private final EventStream<T> source;
    private final Predicate<? super T> predicate;

    public FilterStream(
            EventStream<T> source,
            Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected Subscription observeInputs() {
        return source.subscribe(t -> {
            if(predicate.test(t)) {
                emit(t);
            }
        });
    }
}