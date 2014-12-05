package org.reactfx;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.reactfx.util.MapHelper;

class OnRecurseAccumulateStream<T, A> extends LazilyBoundStream<T> {
    private final EventStream<T> source;
    private final Function<? super T, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super T, ? extends A> reduction;
    private final ToIntFunction<? super A> size;
    private final Function<? super A, ? extends T> head;
    private final Function<? super A, ? extends A> tail;

    private MapHelper<Subscriber<? super T>, A> pendingEvents = null;

    public OnRecurseAccumulateStream(
            EventStream<T> source,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> reduction,
            ToIntFunction<? super A> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail) {
        this.source = source;
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
        this.size = size;
        this.head = head;
        this.tail = tail;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(source, this::emitValue);
    }

    private void emitValue(T value) {
        if(MapHelper.isEmpty(pendingEvents) && getObserverCount() == 1) {
            emit(value);
        } else {
            forEachObserver(s -> {
                if(MapHelper.containsKey(pendingEvents, s)) {
                    A accum = MapHelper.get(pendingEvents, s);
                    accum = reduction.apply(accum, value);
                    if(size.applyAsInt(accum) > 0) {
                        pendingEvents = MapHelper.put(pendingEvents, s, accum);
                    } else {
                        pendingEvents = MapHelper.remove(pendingEvents, s);
                    }
                } else {
                    A accum = initialTransformation.apply(value);
                    if(size.applyAsInt(accum) > 0) {
                        pendingEvents = MapHelper.put(pendingEvents, s, accum);
                    }
                }
            });
            while(!MapHelper.isEmpty(pendingEvents)) {
                Subscriber<? super T> subscriber = MapHelper.chooseKey(pendingEvents);
                A accum = MapHelper.get(pendingEvents, subscriber);
                int n = size.applyAsInt(accum);
                assert n > 0;
                T first = head.apply(accum);
                if(n == 1) {
                    pendingEvents = MapHelper.remove(pendingEvents, subscriber);
                } else {
                    accum = tail.apply(accum);
                    if(size.applyAsInt(accum) > 0) { // always true if size() and tail() fulfill their contract
                        pendingEvents = MapHelper.put(pendingEvents, subscriber, accum);
                    }
                }
                runUnsafeAction(() -> subscriber.onEvent(first));
            }
        }
    }
}
