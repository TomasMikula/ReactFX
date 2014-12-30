package org.reactfx;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;

import org.reactfx.util.AccumulatorSize;
import org.reactfx.util.NotificationAccumulator;

/**
 * An event stream whose emission of events can be suspended temporarily. What
 * events, if any, are emitted when emission is resumed depends on the concrete
 * implementation.
 */
public interface SuspendableEventStream<T> extends EventStream<T>, Suspendable {

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
extends SuspendableBase<Consumer<? super T>, T, A>
implements EventStreamHelpers<T>, SuspendableEventStream<T> {

    protected SuspendableEventStreamBase(
            EventStream<T> source,
            NotificationAccumulator<Consumer<? super T>, T, A> pn) {
        super(source, pn);
    }
}


final class AccumulativeEventStream<T, A> extends SuspendableEventStreamBase<T, A> {
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
}


final class PausableEventStream<T> extends SuspendableEventStreamBase<T, Deque<T>> {

    PausableEventStream(EventStream<T> source) {
        super(source, NotificationAccumulator.queuingStreamNotifications());
    }

    @Override
    protected AccumulatorSize sizeOf(Deque<T> accum) {
        return AccumulatorSize.fromInt(accum.size());
    }

    @Override
    protected T headOf(Deque<T> accum) {
        return accum.getFirst();
    }

    @Override
    protected Deque<T> tailOf(Deque<T> accum) {
        accum.removeFirst();
        return accum;
    }
}


abstract class AbstractReducibleEventStream<T> extends SuspendableEventStreamBase<T, T> {

    protected AbstractReducibleEventStream(
            EventStream<T> source,
            NotificationAccumulator<Consumer<? super T>, T, T> pn) {
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
}


final class ReducibleEventStream<T> extends AbstractReducibleEventStream<T> {

    public ReducibleEventStream(
            EventStream<T> source,
            BinaryOperator<T> reduction) {
        super(source, NotificationAccumulator.reducingStreamNotifications(reduction));
    }
}


final class ForgetfulEventStream<T> extends AbstractReducibleEventStream<T> {

    ForgetfulEventStream(EventStream<T> source) {
        super(source, NotificationAccumulator.retainLatestStreamNotifications());
    }
}


final class SuppressibleEventStream<T> extends SuspendableEventStreamBase<T, T> {

    SuppressibleEventStream(EventStream<T> source) {
        super(source, NotificationAccumulator.nonAccumulativeStreamNotifications());
    }

    @Override
    protected AccumulatorSize sizeOf(T accum) {
        return AccumulatorSize.ZERO;
    }

    @Override
    protected T headOf(T accum) {
        throw new NoSuchElementException();
    }

    @Override
    protected T tailOf(T accum) {
        throw new NoSuchElementException();
    }

    @Override
    protected T initialAccumulator(T value) {
        return null;
    }

    // Override reduce so that it permits accumulation.
    @Override
    protected T reduce(T accum, T value) {
        return null;
    }
}