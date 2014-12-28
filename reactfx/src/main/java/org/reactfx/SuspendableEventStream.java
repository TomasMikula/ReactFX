package org.reactfx;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.value.ObservableValue;

import org.reactfx.util.AccumulatorSize;
import org.reactfx.util.NotificationAccumulator;

/**
 * An event stream whose emission of events can be suspended temporarily. What
 * events, if any, are emitted when emission is resumed depends on the concrete
 * implementation.
 */
public interface SuspendableEventStream<T> extends EventStream<T> {

    Guard suspend();

    default void suspendWhile(Runnable r) {
        try(Guard g = suspend()) { r.run(); }
    };

    default <U> U suspendWhile(Supplier<U> f) {
        try(Guard g = suspend()) { return f.get(); }
    }

    /**
     * Returns an event stream that is suspended when the given
     * {@code condition} is {@code true} and emits normally when
     * {@code condition} is {@code false}.
     */
    default EventStream<T> suspendWhen(ObservableValue<Boolean> condition) {
        return new SuspendWhenStream<>(this, condition);
    }
}

abstract class SuspendableEventStreamBase<T, A>
extends EventStreamBase<T>
implements SuspendableEventStream<T> {

    private final EventStream<T> source;

    private int suspended = 0;
    private boolean hasValue = false;
    private A accumulatedValue = null;

    protected SuspendableEventStreamBase(
            EventStream<T> source,
            NotificationAccumulator<Subscriber<? super T>, T> pn) {
        super(pn);
        this.source = source;
    }

    protected abstract AccumulatorSize sizeOf(A accum);
    protected abstract T headOf(A accum);
    protected abstract A tailOf(A accum);
    protected abstract A initialAccumulator(T event);
    protected abstract A reduce(A accum, T event);

    protected final boolean isSuspended() {
        return suspended > 0;
    }

    @Override
    public final Guard suspend() {
        ++suspended;
        return Guard.closeableOnce(this::resume);
    }

    @Override
    protected final Subscription bindToInputs() {
        Subscription sub = subscribeTo(source, this::handleEvent);
        return sub.and(this::reset);
    }

    private void resume() {
        --suspended;
        if(suspended == 0 && hasValue) {
            while(sizeOf(accumulatedValue) == AccumulatorSize.MANY) {
                enqueueNotifications(headOf(accumulatedValue));
                accumulatedValue = tailOf(accumulatedValue);
            }
            if(sizeOf(accumulatedValue) == AccumulatorSize.ONE) {
                enqueueNotifications(headOf(accumulatedValue));
            }
            reset();
            notifyObservers();
        }
    }

    private void reset() {
        hasValue = false;
        accumulatedValue = null;
    }

    private void handleEvent(T event) {
        if(isSuspended()) {
            if(hasValue) {
                accumulatedValue = reduce(accumulatedValue, event);
            } else {
                accumulatedValue = initialAccumulator(event);
                hasValue = true;
            }
        } else {
            emit(event);
        }
    }
}


final class AccumulativeEventStream<T, A> extends SuspendableEventStreamBase<T, A> {
    private final Function<? super T, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super T, ? extends A> accumulation;
    private final Function<? super A, AccumulatorSize> size;
    private final Function<? super A, ? extends T> head;
    private final Function<? super A, ? extends A> tail;

    AccumulativeEventStream(
            EventStream<T> source,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail) {
        super(source, NotificationAccumulator.accumulativeStreamNotifications(size, head, tail, initialTransformation, accumulation));
        this.initialTransformation = initialTransformation;
        this.accumulation = accumulation;
        this.size = size;
        this.head = head;
        this.tail = tail;
    }

    @Override
    protected AccumulatorSize sizeOf(A accum) {
        return size.apply(accum);
    }

    @Override
    protected T headOf(A accum) {
        return head.apply(accum);
    }

    @Override
    protected A tailOf(A accum) {
        return tail.apply(accum);
    }

    @Override
    protected A initialAccumulator(T event) {
        return initialTransformation.apply(event);
    }

    @Override
    protected A reduce(A accum, T event) {
        return accumulation.apply(accum, event);
    }
}


final class PausableEventStream<T> extends SuspendableEventStreamBase<T, List<T>> {

    PausableEventStream(EventStream<T> source) {
        super(source, NotificationAccumulator.queuingStreamNotifications());
    }

    @Override
    protected AccumulatorSize sizeOf(List<T> accum) {
        return AccumulatorSize.fromInt(accum.size());
    }

    @Override
    protected T headOf(List<T> accum) {
        return accum.get(0);
    }

    @Override
    protected List<T> tailOf(List<T> accum) {
        accum.remove(0);
        return accum;
    }

    @Override
    protected List<T> initialAccumulator(T event) {
        ArrayList<T> res = new ArrayList<>();
        res.add(event);
        return res;
    }

    @Override
    protected List<T> reduce(List<T> accum, T event) {
        accum.add(event);
        return accum;
    }
}


abstract class AbstractReducibleEventStream<T> extends SuspendableEventStreamBase<T, T> {

    protected AbstractReducibleEventStream(
            EventStream<T> source,
            NotificationAccumulator<Subscriber<? super T>, T> pn) {
        super(source, pn);
    }

    @Override
    protected final AccumulatorSize sizeOf(T accum) {
        return AccumulatorSize.ONE;
    }

    @Override
    protected final T headOf(T accum) {
        return accum;
    }

    @Override
    protected final T tailOf(T accum) {
        throw new NoSuchElementException();
    }

    @Override
    protected final T initialAccumulator(T event) {
        return event;
    }
}


final class ReducibleEventStream<T> extends AbstractReducibleEventStream<T> {
    private final BinaryOperator<T> reduction;

    public ReducibleEventStream(
            EventStream<T> source,
            BinaryOperator<T> reduction) {
        super(source, NotificationAccumulator.reducingStreamNotifications(reduction));
        this.reduction = reduction;
    }

    @Override
    protected T reduce(T accum, T event) {
        return reduction.apply(accum, event);
    }
}


final class ForgetfulEventStream<T> extends AbstractReducibleEventStream<T> {

    ForgetfulEventStream(EventStream<T> source) {
        super(source, NotificationAccumulator.retainLatestStreamNotifications());
    }

    @Override
    protected T reduce(T accum, T event) {
        return event;
    }
}


final class SuppressibleEventStream<T> extends SuspendableEventStreamBase<T, Void> {

    SuppressibleEventStream(EventStream<T> source) {
        super(source, NotificationAccumulator.nonRecursiveStreamNotifications());
    }

    @Override
    protected AccumulatorSize sizeOf(Void accum) {
        return AccumulatorSize.ZERO;
    }

    @Override
    protected T headOf(Void accum) {
        throw new NoSuchElementException();
    }

    @Override
    protected Void tailOf(Void accum) {
        throw new NoSuchElementException();
    }

    @Override
    protected Void initialAccumulator(T event) {
        return null;
    }

    @Override
    protected Void reduce(Void accum, T event) {
        return null;
    }
}