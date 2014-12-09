package org.reactfx;

import static org.reactfx.util.Tuples.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.reactfx.util.Tuple3;

/**
 *
 * @param <O> type of the observer
 * @param <V> type of the value
 */
interface PendingNotifications<O, V> extends AutoCloseable {
    boolean isEmpty();
    Tuple3<O, V, PendingNotifications<O, V>> takeOne();
    PendingNotifications<O, V> addAll(Iterator<O> observers, V value);

    /**
     * Clears all pending notifications.
     */
    @Override
    void close();
}


abstract class EmptyPendingNotifications<O, V>
implements PendingNotifications<O, V> {

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Tuple3<O, V, PendingNotifications<O, V>> takeOne() {
        throw new NoSuchElementException();
    }

    @Override
    public void close() {
        // do nothing
    }
}


abstract class SingleIterationPendingNotifications<O, V>
implements PendingNotifications<O, V> {

    private Iterator<O> it;
    private V value;

    SingleIterationPendingNotifications(Iterator<O> observers, V value) {
        this.it = observers;
        this.value = value;
    }

    protected Iterator<O> getObservers() { return it; }
    protected V getValue() { return value; }

    protected abstract EmptyPendingNotifications<O, V> empty();
    protected abstract PendingNotifications<O, V> merge(Iterator<O> observers, V value);

    @Override
    public boolean isEmpty() {
        return !it.hasNext();
    }

    @Override
    public Tuple3<O, V, PendingNotifications<O, V>> takeOne() {
        O observer = it.next();
        return t(observer, value, it.hasNext() ? this : empty());
    }

    @Override
    public PendingNotifications<O, V> addAll(Iterator<O> observers, V value) {
        if(isEmpty()) { // may happen if depleted by close()
            this.it = observers;
            this.value = value;
            return this;
        } else if(!observers.hasNext()) {
            return this;
        } else {
            return merge(observers, value);
        }
    }

    @Override
    public void close() {
        // deplete the observers
        while(it.hasNext()) it.next();
    }
}


/* ************* *
 * Non-recursive *
 * ************* */

class EmptyNonRecursivePN<O, V> extends EmptyPendingNotifications<O, V> {

    private static EmptyPendingNotifications<?, ?> EMPTY = new EmptyNonRecursivePN<>();

    @SuppressWarnings("unchecked")
    static <O, V> EmptyPendingNotifications<O, V> empty() {
        return (EmptyPendingNotifications<O, V>) EMPTY;
    }

    // private constructor to prevent instantiation
    private EmptyNonRecursivePN() {}

    @Override
    public PendingNotifications<O, V> addAll(Iterator<O> observers, V value) {
        return new SingleIterationNonRecursivePN<O, V>(observers, value);
    }
}

class SingleIterationNonRecursivePN<O, V> extends SingleIterationPendingNotifications<O, V> {

    SingleIterationNonRecursivePN(Iterator<O> observers, V value) {
        super(observers, value);
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return EmptyNonRecursivePN.empty();
    }

    @Override
    protected PendingNotifications<O, V> merge(Iterator<O> observers, V value) {
        throw new IllegalStateException("Cannot recursively notify"
                + " observers before all observers were notified of"
                + " the previous event");
    }
}


/* ************ *
 * Accumulating *
 * ************ */

enum AccumulatorSize {
    ZERO, ONE, MANY;

    public static AccumulatorSize fromInt(int n) {
        if(n < 0) {
            throw new IllegalArgumentException("Size cannot be negative: " + n);
        } else switch(n) {
            case 0: return ZERO;
            case 1: return ONE;
            default: return MANY;
        }
    }
}

abstract class SingleIterationAccumulatingPN<O, V, A>
extends SingleIterationPendingNotifications<O, V> {

    SingleIterationAccumulatingPN(Iterator<O> observers, V value) {
        super(observers, value);
    }

    protected abstract PendingNotifications<O, V> emptyComplex();

    @Override
    protected PendingNotifications<O, V> merge(Iterator<O> obs, V val) {
        PendingNotifications<O, V> res = emptyComplex();
        res.addAll(getObservers(), getValue());
        res.addAll(obs, val);
        return res;
    }
}

abstract class ComplexAccumulatingPN<O, V, A>
implements PendingNotifications<O, V> {
    private final Map<O, A> pendingNotifications = new HashMap<>();

    protected abstract AccumulatorSize size(A accum);
    protected abstract V head(A accum);
    protected abstract A tail(A accum);
    protected abstract A initialAccumulator(V value);
    protected abstract A reduce(A accum, V value);
    protected abstract EmptyPendingNotifications<O, V> empty();

    @Override
    public boolean isEmpty() {
        return pendingNotifications.isEmpty();
    }

    @Override
    public Tuple3<O, V, PendingNotifications<O, V>> takeOne() {
        O observer = pickObserver();
        A accum = pendingNotifications.get(observer);
        AccumulatorSize n = size(accum);
        V first = head(accum);
        switch(n) {
            case ZERO: throw new AssertionError("Unreachable code");
            case ONE: pendingNotifications.remove(observer); break;
            case MANY:
                accum = tail(accum);
                if(size(accum) == AccumulatorSize.ZERO) {
                    throw new RuntimeException("tail() and size() don't obey the contract: tail of MANY cannot be ZERO");
                }
                pendingNotifications.put(observer, accum);
                break;
        }
        return t(observer, first, isEmpty() ? empty() : this);
    }

    @Override
    public PendingNotifications<O, V> addAll(Iterator<O> observers, V value) {
        while(observers.hasNext()) {
            O observer = observers.next();
            if(pendingNotifications.containsKey(observer)) {
                A accum = pendingNotifications.get(observer);
                accum = reduce(accum, value);
                if(size(accum) != AccumulatorSize.ZERO) {
                    pendingNotifications.put(observer, accum);
                } else {
                    pendingNotifications.remove(observer);
                }
            } else {
                A accum = initialAccumulator(value);
                if(size(accum) != AccumulatorSize.ZERO) {
                    pendingNotifications.put(observer, accum);
                }
            }
        }
        return this;
    }

    @Override
    public void close() {
        pendingNotifications.clear();
    }

    private O pickObserver() {
        return pendingNotifications.keySet().iterator().next();
    }
}


/* ******************** *
 * General Accumulating *
 * ******************** */

final class EmptyGeneralAccumulatingPN<O, V, A>
extends EmptyPendingNotifications<O, V> {
    private final Function<? super A, AccumulatorSize> size;
    private final Function<? super A, ? extends V> head;
    private final Function<? super A, ? extends A> tail;
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    EmptyGeneralAccumulatingPN(
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends V> head,
            Function<? super A, ? extends A> tail,
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction) {
        this.size = size;
        this.head = head;
        this.tail = tail;
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
    }

    @Override
    public PendingNotifications<O, V> addAll(Iterator<O> observers, V value) {
        return new SingleIterationGeneralAccumulatingPN<>(
                size, head, tail, initialTransformation, reduction,
                observers, value);
    }
}

final class SingleIterationGeneralAccumulatingPN<O, V, A>
extends SingleIterationAccumulatingPN<O, V, A> {
    private final Function<? super A, AccumulatorSize> size;
    private final Function<? super A, ? extends V> head;
    private final Function<? super A, ? extends A> tail;
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    SingleIterationGeneralAccumulatingPN(
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends V> head,
            Function<? super A, ? extends A> tail,
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction,
            Iterator<O> observers,
            V value) {
        super(observers, value);
        this.size = size;
        this.head = head;
        this.tail = tail;
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
    }

    @Override
    protected PendingNotifications<O, V> emptyComplex() {
        return new ComplexGeneralAccumulatingPN<>(
                size, head, tail, initialTransformation, reduction);
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return new EmptyGeneralAccumulatingPN<>(
                size, head, tail, initialTransformation, reduction);
    }
}

final class ComplexGeneralAccumulatingPN<O, V, A>
extends ComplexAccumulatingPN<O, V, A> {
    private final Function<? super A, AccumulatorSize> size;
    private final Function<? super A, ? extends V> head;
    private final Function<? super A, ? extends A> tail;
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    ComplexGeneralAccumulatingPN(
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends V> head,
            Function<? super A, ? extends A> tail,
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction) {
        this.size = size;
        this.head = head;
        this.tail = tail;
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
    }

    @Override
    protected AccumulatorSize size(A accum) {
        return size.apply(accum);
    }

    @Override
    protected V head(A accum) {
        return head.apply(accum);
    }

    @Override
    protected A tail(A accum) {
        return tail.apply(accum);
    }

    @Override
    protected A initialAccumulator(V value) {
        return initialTransformation.apply(value);
    }

    @Override
    protected A reduce(A accum, V value) {
        return reduction.apply(accum, value);
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return new EmptyGeneralAccumulatingPN<>(
                size, head, tail, initialTransformation, reduction);
    }
}


/* ******** *
 * Reducing *
 * ******** */

abstract class ComplexReducingPN<O, V>
extends ComplexAccumulatingPN<O, V, V> {

    @Override
    protected AccumulatorSize size(V accum) {
        return AccumulatorSize.ONE;
    }

    @Override
    protected V head(V accum) {
        return accum;
    }

    @Override
    protected V tail(V accum) {
        throw new NoSuchElementException();
    }

    @Override
    protected V initialAccumulator(V value) {
        return value;
    }
}


/* **************** *
 * General Reducing *
 * **************** */

final class EmptyGeneralReducingPN<O, V> extends EmptyPendingNotifications<O, V> {
    private final BinaryOperator<V> reduction;

    EmptyGeneralReducingPN(BinaryOperator<V> reduction) {
        this.reduction = reduction;
    }

    @Override
    public PendingNotifications<O, V> addAll(Iterator<O> observers, V value) {
        return new SingleIterationGeneralReducingPN<>(reduction, observers, value);
    }
}

final class SingleIterationGeneralReducingPN<O, V>
extends SingleIterationAccumulatingPN<O, V, V> {
    private final BinaryOperator<V> reduction;

    SingleIterationGeneralReducingPN(
            BinaryOperator<V> reduction,
            Iterator<O> observers, V value) {
        super(observers, value);
        this.reduction = reduction;
    }

    @Override
    protected PendingNotifications<O, V> emptyComplex() {
        return new ComplexGeneralReducingPN<O, V>(reduction);
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return new EmptyGeneralReducingPN<>(reduction);
    }
}

final class ComplexGeneralReducingPN<O, V> extends ComplexReducingPN<O, V> {
    private final BinaryOperator<V> reduction;

    public ComplexGeneralReducingPN(BinaryOperator<V> reduction) {
        this.reduction = reduction;
    }

    @Override
    protected V reduce(V accum, V value) {
        return reduction.apply(accum, value);
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return new EmptyGeneralReducingPN<>(reduction);
    }

}


/* ************* *
 * Retain Oldest *
 * ************* */

final class EmptyRetainOldestPN<O, V> extends EmptyPendingNotifications<O, V> {

    private static final EmptyPendingNotifications<?, ?> EMPTY = new EmptyRetainOldestPN<>();

    @SuppressWarnings("unchecked")
    static <O, V> EmptyPendingNotifications<O, V> empty() {
        return (EmptyPendingNotifications<O, V>) EMPTY;
    }

    // private constructor to prevent instantiation
    private EmptyRetainOldestPN() {}

    @Override
    public PendingNotifications<O, V> addAll(Iterator<O> observers, V value) {
        return new SingleIterationRetainOldestPN<>(observers, value);
    }
}

final class SingleIterationRetainOldestPN<O, V>
extends SingleIterationAccumulatingPN<O, V, V> {

    SingleIterationRetainOldestPN(Iterator<O> observers, V value) {
        super(observers, value);
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return EmptyRetainOldestPN.empty();
    }

    @Override
    protected PendingNotifications<O, V> emptyComplex() {
        return new ComplexRetainOldestPN<>();
    }
}

final class ComplexRetainOldestPN<O, V>
extends ComplexReducingPN<O, V> {

    @Override
    protected V reduce(V accum, V value) {
        return accum;
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return EmptyRetainOldestPN.empty();
    }
}


/* ************* *
 * Retain Latest *
 * ************* */

final class EmptyRetainLatestPN<O, V> extends EmptyPendingNotifications<O, V> {

    private static final EmptyPendingNotifications<?, ?> EMPTY = new EmptyRetainLatestPN<>();

    @SuppressWarnings("unchecked")
    static <O, V> EmptyPendingNotifications<O, V> empty() {
        return (EmptyPendingNotifications<O, V>) EMPTY;
    }

    // private constructor to prevent instantiation
    private EmptyRetainLatestPN() {}

    @Override
    public PendingNotifications<O, V> addAll(Iterator<O> observers, V value) {
        return new SingleIterationRetainLatestPN<>(observers, value);
    }
}

final class SingleIterationRetainLatestPN<O, V> extends SingleIterationAccumulatingPN<O, V, V> {

    SingleIterationRetainLatestPN(Iterator<O> observers, V value) {
        super(observers, value);
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return EmptyRetainLatestPN.empty();
    }

    @Override
    protected PendingNotifications<O, V> emptyComplex() {
        return new ComplexRetainLatestPN<>();
    }
}

final class ComplexRetainLatestPN<O, V>
extends ComplexReducingPN<O, V> {

    @Override
    protected V reduce(V accum, V value) {
        return value;
    }

    @Override
    protected EmptyPendingNotifications<O, V> empty() {
        return EmptyRetainLatestPN.empty();
    }
}