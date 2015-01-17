package org.reactfx.util;

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactfx.collection.ListModificationSequence;
import org.reactfx.collection.ObsList;
import org.reactfx.collection.ObsList.Observer;
import org.reactfx.collection.QuasiListChange;

/**
 * @param <O> observer type
 * @param <V> type of produced values
 * @param <A> type of accumulated value
 */
public interface NotificationAccumulator<O, V, A> {

    static <T, A> NotificationAccumulator<Consumer<? super T>, T, A> accumulativeStreamNotifications(
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> reduction) {
        return new AccumulativeStreamNotifications<>(
                size, head, tail, initialTransformation, reduction);
    }

    static <T> NotificationAccumulator<Consumer<? super T>, T, Deque<T>> queuingStreamNotifications() {
        return new QueuingStreamNotifications<>();
    }

    static <T> NotificationAccumulator<Consumer<? super T>, T, T> reducingStreamNotifications(BinaryOperator<T> reduction) {
        return new ReducingStreamNotifications<>(reduction);
    }

    static <T> NotificationAccumulator<Consumer<? super T>, T, T> retainLatestStreamNotifications() {
        return new RetainLatestStreamNotifications<>();
    }

    static <T> NotificationAccumulator<Consumer<? super T>, T, T> retainOldestValNotifications() {
        return new RetailOldestValNotifications<>();
    }

    static <T> NotificationAccumulator<Consumer<? super T>, T, T> nonAccumulativeStreamNotifications() {
        return new NonAccumulativeStreamNotifications<>();
    }

    static <E> NotificationAccumulator<ObsList.Observer<? super E, ?>, QuasiListChange<? extends E>, ListModificationSequence<E>> listNotifications() {
        return new ListNotifications<>();
    }


    /* Interface methods */

    boolean isEmpty();
    Runnable takeOne();
    void addAll(Iterator<O> observers, V value);
    void clear();
    AccumulationFacility<V, A> getAccumulationFacility();
}

abstract class NotificationAccumulatorBase<O, V, A>
implements NotificationAccumulator<O, V, A>, AccumulationFacility<V, A> {
    private AccuMap<O, V, A> accuMap = AccuMap.empty();

    protected abstract AccumulatorSize size(O observer, A accumulatedValue);
    protected abstract Runnable head(O observer, A accumulatedValue);
    protected abstract A tail(O observer, A accumulatedValue);

    @Override
    public AccumulationFacility<V, A> getAccumulationFacility() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return accuMap.isEmpty();
    }

    @Override
    public Runnable takeOne() {
        Tuple2<O, A> t = accuMap.peek(this);
        switch(t.map(this::size)) {
            case ZERO:
                accuMap = accuMap.dropPeeked();
                return () -> {};
            case ONE:
                accuMap = accuMap.dropPeeked();
                return t.map(this::head);
            case MANY:
                Runnable notification = t.map(this::head);
                A newAccumulatedValue = t.map(this::tail);
                accuMap = accuMap.updatePeeked(newAccumulatedValue);
                return notification;
            default:
                throw new AssertionError("Unreachable code");
        }
    }

    @Override
    public void addAll(Iterator<O> keys, V value) {
        accuMap = accuMap.addAll(keys, value, this);
    }

    @Override
    public void clear() {
        accuMap = AccuMap.empty();
    }
}


/* ******************** *
 * Non-recursive stream *
 * ******************** */

final class NonAccumulativeStreamNotifications<T>
extends NotificationAccumulatorBase<Consumer<? super T>, T, T>
implements AccumulationFacility.NoAccumulation<T> {

    @Override
    protected AccumulatorSize size(
            Consumer<? super T> observer,
            T accumulatedValue) {
        return AccumulatorSize.ONE;
    }

    @Override
    protected Runnable head(Consumer<? super T> observer, T accumulatedValue) {
        return () -> observer.accept(accumulatedValue);
    }

    @Override
    protected T tail(Consumer<? super T> observer, T accumulatedValue) {
        throw new NoSuchElementException();
    }
}


/* ******************* *
 * Accumulative stream *
 * ******************* */

final class AccumulativeStreamNotifications<T, A>
extends NotificationAccumulatorBase<Consumer<? super T>, T, A> {
    private final Function<? super A, AccumulatorSize> size;
    private final Function<? super A, ? extends T> head;
    private final Function<? super A, ? extends A> tail;
    private final Function<? super T, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super T, ? extends A> reduction;

    AccumulativeStreamNotifications(
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> reduction) {
        this.size = size;
        this.head = head;
        this.tail = tail;
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
    }

    @Override
    protected AccumulatorSize size(Consumer<? super T> observer, A accumulatedValue) {
        return size.apply(accumulatedValue);
    }

    @Override
    protected Runnable head(Consumer<? super T> observer, A accumulatedValue) {
        T event = head.apply(accumulatedValue);
        return () -> observer.accept(event);
    }

    @Override
    protected A tail(Consumer<? super T> observer, A accumulatedValue) {
        return tail.apply( accumulatedValue);
    }

    @Override
    public A initialAccumulator(T value) {
        return initialTransformation.apply(value);
    }

    @Override
    public A reduce(A accum, T value) {
        return reduction.apply(accum, value);
    }
}


/* ************** *
 * Queuing stream *
 * ************** */

final class QueuingStreamNotifications<T>
extends NotificationAccumulatorBase<Consumer<? super T>, T, Deque<T>>
implements AccumulationFacility.Queuing<T> {

    @Override
    protected AccumulatorSize size(
            Consumer<? super T> observer,
            Deque<T> accumulatedValue) {
        return AccumulatorSize.fromInt(accumulatedValue.size());
    }

    @Override
    protected Runnable head(
            Consumer<? super T> observer,
            Deque<T> accumulatedValue) {
        T t = accumulatedValue.getFirst();
        return () -> observer.accept(t);
    }

    @Override
    protected Deque<T> tail(
            Consumer<? super T> observer,
            Deque<T> accumulatedValue) {
        accumulatedValue.removeFirst();
        return accumulatedValue;
    }

}


/* *************** *
 * Reducing stream *
 * *************** */

abstract class AbstractReducingStreamNotifications<T>
extends NotificationAccumulatorBase<Consumer<? super T>, T, T>
implements AccumulationFacility.HomotypicAccumulation<T> {

    @Override
    protected final AccumulatorSize size(
            Consumer<? super T> observer,
            T accumulatedValue) {
        return AccumulatorSize.ONE;
    }

    @Override
    protected final Runnable head(Consumer<? super T> observer, T accumulatedValue) {
        return () -> observer.accept(accumulatedValue);
    }

    @Override
    protected final T tail(Consumer<? super T> observer, T accumulatedValue) {
        throw new NoSuchElementException();
    }
}

final class ReducingStreamNotifications<T>
extends AbstractReducingStreamNotifications<T> {
    private final BinaryOperator<T> reduction;

    ReducingStreamNotifications(BinaryOperator<T> reduction) {
        this.reduction = reduction;
    }

    @Override
    public T reduce(T accum, T value) {
        return reduction.apply(accum, value);
    }
}


/* ******************** *
 * Retain Latest stream *
 * ******************** */

final class RetainLatestStreamNotifications<T>
extends AbstractReducingStreamNotifications<T>
implements AccumulationFacility.RetainLatest<T> {}


/* ***************** *
 * Retain Oldest Val *
 * ***************** */

final class RetailOldestValNotifications<T>
extends AbstractReducingStreamNotifications<T>
implements AccumulationFacility.RetainOldest<T> {}



/* ************************ *
 * List change accumulation *
 * ************************ */

final class ListNotifications<E>
extends NotificationAccumulatorBase<ObsList.Observer<? super E, ?>, QuasiListChange<? extends E>, ListModificationSequence<E>>
implements AccumulationFacility.ListChangeAccumulation<E> {

    @Override
    protected AccumulatorSize size(
            Observer<? super E, ?> observer,
            ListModificationSequence<E> accumulatedValue) {
        return observer.sizeOf(accumulatedValue);
    }

    @Override
    protected Runnable head(
            Observer<? super E, ?> observer,
            ListModificationSequence<E> mods) {
        return takeHead(observer, mods);
    }

    private final <O> Runnable takeHead(
            Observer<? super E, O> observer,
            ListModificationSequence<E> mods) {
        O h = observer.headOf(mods);
        return () -> observer.onChange(h);
    }

    @Override
    protected ListModificationSequence<E> tail(
            Observer<? super E, ?> observer,
            ListModificationSequence<E> mods) {
        return observer.tailOf(mods);
    }
}