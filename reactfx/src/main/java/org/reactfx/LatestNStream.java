package org.reactfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.reactfx.util.Lists;

public class LatestNStream<T> extends EventStreamBase<List<T>> {
    private final EventStream<T> source;
    private final int n;

    private List<T> first = null;
    private List<T> second = null;
    private List<T> concatView = null;

    public LatestNStream(EventStream<T> source, int n) {
        if(n <= 0) {
            throw new IllegalArgumentException("n must be positive. Was " + n);
        }

        this.source = source;
        this.n = n;
    }

    @Override
    protected Subscription observeInputs() {
        first = Collections.emptyList();
        second = new ArrayList<>(n);
        concatView = Lists.concatView(first, second);
        return source.subscribe(this::onEvent);
    }

    private void onEvent(T event) {
        if(second.size() == n) {
            first = second;
            second = new ArrayList<>(n);
            concatView = Lists.concatView(first, second);
        }
        second.add(event);
        int total = concatView.size();
        emit(concatView.subList(Math.max(0, total - n), total));
    }
}