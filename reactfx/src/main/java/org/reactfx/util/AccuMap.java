package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.reactfx.collection.ListChangeAccumulator;
import org.reactfx.collection.ListChange;

/**
 * Accumulation map.
 *
 * @param <K> key type
 * @param <V> type of individual (non-accumulated) values
 */
public interface AccuMap<K, V> {

    /**
     * Immutable empty accumulation map.
     * {@link #addAll(Iterator, Object)} must return a new map, because this
     * instance is immutable.
     */
    static abstract class Empty<K, V> implements AccuMap<K, V> {

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Tuple3<K, V, AccuMap<K, V>> takeOne() {
            throw new NoSuchElementException();
        }

        @Override
        public AccuMap<K, V> empty() {
            return this;
        }
    }

    static <K, V, A> Empty<K, V> emptyAccumulationMap(
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends V> head,
            Function<? super A, ? extends A> tail,
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction) {
        return new EmptyGeneralAccumulationMap<>(
                size, head, tail, initialTransformation, reduction);
    }

    static <K, V> Empty<K, V> emptyReductionMap(BinaryOperator<V> reduction) {
        return new EmptyGeneralReductionMap<>(reduction);
    }

    static <K, V> Empty<K, V> emptyRetainLatestMap() {
        return EmptyRetainLatestMap.instance();
    }

    static <K, V> Empty<K, V> emptyRetainOldestMap() {
        return EmptyRetainOldestMap.instance();
    }

    static <K, V> Empty<K, V> emptyNonAdditiveMap() {
        return EmptyNonAdditiveMap.instance();
    }

    static <K, E> Empty<K, ListChange<? extends E>> emptyListChangeAccumulationMap() {
        return EmptyListChangeAccuMap.instance();
    }


    /* Interface methods */

    boolean isEmpty();
    Tuple3<K, V, AccuMap<K, V>> takeOne();
    AccuMap<K, V> addAll(Iterator<K> keys, V value);
    AccuMap<K, V> empty();
}


abstract class SingleIterationAccuMap<K, V>
implements AccuMap<K, V> {

    private Iterator<K> it;
    private V value;

    SingleIterationAccuMap(Iterator<K> keys, V value) {
        this.it = keys;
        this.value = value;
    }

    protected Iterator<K> getKeys() { return it; }
    protected V getValue() { return value; }

    protected abstract AccuMap<K, V> merge(Iterator<K> keys, V value);

    @Override
    public boolean isEmpty() {
        return !it.hasNext();
    }

    @Override
    public Tuple3<K, V, AccuMap<K, V>> takeOne() {
        K observer = it.next();
        return t(observer, value, it.hasNext() ? this : empty());
    }

    @Override
    public AccuMap<K, V> addAll(Iterator<K> keys, V value) {
        if(isEmpty()) {
            this.it = keys;
            this.value = value;
            return this;
        } else if(!keys.hasNext()) {
            return this;
        } else {
            return merge(keys, value);
        }
    }
}


/* ************ *
 * Non-additive *
 * ************ */

class EmptyNonAdditiveMap<K, V> extends AccuMap.Empty<K, V> {

    private static AccuMap.Empty<?, ?> INSTANCE = new EmptyNonAdditiveMap<>();

    @SuppressWarnings("unchecked")
    static <K, V> AccuMap.Empty<K, V> instance() {
        return (AccuMap.Empty<K, V>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyNonAdditiveMap() {}

    @Override
    public AccuMap<K, V> addAll(Iterator<K> keys, V value) {
        return new SingleIterationNonAdditiveMap<K, V>(keys, value);
    }
}

class SingleIterationNonAdditiveMap<K, V> extends SingleIterationAccuMap<K, V> {

    SingleIterationNonAdditiveMap(Iterator<K> keys, V value) {
        super(keys, value);
    }

    @Override
    public AccuMap.Empty<K, V> empty() {
        return EmptyNonAdditiveMap.instance();
    }

    @Override
    protected AccuMap<K, V> merge(Iterator<K> keys, V value) {
        throw new IllegalStateException("Cannot accept additional"
                + " entries before removing all previous entries.");
    }
}


/* ************ *
 * Accumulative *
 * ************ */

abstract class SingleIterationAccumulationMap<K, V, A>
extends SingleIterationAccuMap<K, V> {

    SingleIterationAccumulationMap(Iterator<K> keys, V value) {
        super(keys, value);
    }

    protected abstract AccuMap<K, V> emptyComplex();

    @Override
    protected AccuMap<K, V> merge(Iterator<K> keys, V val) {
        AccuMap<K, V> res = emptyComplex();
        res.addAll(getKeys(), getValue());
        res.addAll(keys, val);
        return res;
    }
}

@SuppressWarnings("serial")
abstract class AccumulationMap<K, V, A> extends HashMap<K, A> implements AccuMap<K, V> {

    protected abstract AccumulatorSize size(A accum);
    protected abstract V head(A accum);
    protected abstract A tail(A accum);
    protected abstract A initialAccumulator(V value);
    protected abstract A reduce(A accum, V value);

    @Override
    public Tuple3<K, V, AccuMap<K, V>> takeOne() {
        K observer = pickKey();
        A accum = this.get(observer);
        AccumulatorSize n = size(accum);
        V first = head(accum);
        switch(n) {
            case ZERO: throw new AssertionError("Unreachable code");
            case ONE: this.remove(observer); break;
            case MANY:
                accum = tail(accum);
                if(size(accum) == AccumulatorSize.ZERO) {
                    throw new RuntimeException("tail() and size() don't obey the contract: tail of MANY cannot be ZERO");
                }
                this.put(observer, accum);
                break;
        }
        return t(observer, first, isEmpty() ? empty() : this);
    }

    @Override
    public AccuMap<K, V> addAll(Iterator<K> keys, V value) {
        while(keys.hasNext()) {
            K observer = keys.next();
            if(this.containsKey(observer)) {
                A accum = this.get(observer);
                accum = reduce(accum, value);
                if(size(accum) != AccumulatorSize.ZERO) {
                    this.put(observer, accum);
                } else {
                    this.remove(observer);
                }
            } else {
                A accum = initialAccumulator(value);
                if(size(accum) != AccumulatorSize.ZERO) {
                    this.put(observer, accum);
                }
            }
        }
        return this;
    }

    private K pickKey() {
        return this.keySet().iterator().next();
    }
}


/* ******************** *
 * General Accumulation *
 * ******************** */

final class EmptyGeneralAccumulationMap<K, V, A> extends AccuMap.Empty<K, V> {
    private final Function<? super A, AccumulatorSize> size;
    private final Function<? super A, ? extends V> head;
    private final Function<? super A, ? extends A> tail;
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    EmptyGeneralAccumulationMap(
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
    public AccuMap<K, V> addAll(Iterator<K> keys, V value) {
        return new SingleIterationGeneralAccumulationMap<>(
                size, head, tail, initialTransformation, reduction,
                keys, value);
    }
}

final class SingleIterationGeneralAccumulationMap<K, V, A>
extends SingleIterationAccumulationMap<K, V, A> {
    private final Function<? super A, AccumulatorSize> size;
    private final Function<? super A, ? extends V> head;
    private final Function<? super A, ? extends A> tail;
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    SingleIterationGeneralAccumulationMap(
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends V> head,
            Function<? super A, ? extends A> tail,
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction,
            Iterator<K> keys,
            V value) {
        super(keys, value);
        this.size = size;
        this.head = head;
        this.tail = tail;
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
    }

    @Override
    protected AccuMap<K, V> emptyComplex() {
        return new GeneralAccumulationMap<>(
                size, head, tail, initialTransformation, reduction);
    }

    @Override
    public AccuMap.Empty<K, V> empty() {
        return new EmptyGeneralAccumulationMap<>(
                size, head, tail, initialTransformation, reduction);
    }
}

@SuppressWarnings("serial")
final class GeneralAccumulationMap<K, V, A>
extends AccumulationMap<K, V, A> {
    private final Function<? super A, AccumulatorSize> size;
    private final Function<? super A, ? extends V> head;
    private final Function<? super A, ? extends A> tail;
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    GeneralAccumulationMap(
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
    public AccuMap.Empty<K, V> empty() {
        return new EmptyGeneralAccumulationMap<>(
                size, head, tail, initialTransformation, reduction);
    }
}


/* ******** *
 * Reducing *
 * ******** */

@SuppressWarnings("serial")
abstract class ReductionMap<K, V>
extends AccumulationMap<K, V, V> {

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


/* ***************** *
 * General Reduction *
 * ***************** */

final class EmptyGeneralReductionMap<K, V> extends AccuMap.Empty<K, V> {
    private final BinaryOperator<V> reduction;

    EmptyGeneralReductionMap(BinaryOperator<V> reduction) {
        this.reduction = reduction;
    }

    @Override
    public AccuMap<K, V> addAll(Iterator<K> keys, V value) {
        return new SingleIterationGeneralReductionMap<>(reduction, keys, value);
    }
}

final class SingleIterationGeneralReductionMap<K, V>
extends SingleIterationAccumulationMap<K, V, V> {
    private final BinaryOperator<V> reduction;

    SingleIterationGeneralReductionMap(
            BinaryOperator<V> reduction,
            Iterator<K> keys, V value) {
        super(keys, value);
        this.reduction = reduction;
    }

    @Override
    protected AccuMap<K, V> emptyComplex() {
        return new GeneralReductionMap<K, V>(reduction);
    }

    @Override
    public AccuMap.Empty<K, V> empty() {
        return new EmptyGeneralReductionMap<>(reduction);
    }
}

@SuppressWarnings("serial")
final class GeneralReductionMap<K, V> extends ReductionMap<K, V> {
    private final BinaryOperator<V> reduction;

    public GeneralReductionMap(BinaryOperator<V> reduction) {
        this.reduction = reduction;
    }

    @Override
    protected V reduce(V accum, V value) {
        return reduction.apply(accum, value);
    }

    @Override
    public AccuMap.Empty<K, V> empty() {
        return new EmptyGeneralReductionMap<>(reduction);
    }

}


/* ************* *
 * Retain Oldest *
 * ************* */

final class EmptyRetainOldestMap<K, V> extends AccuMap.Empty<K, V> {

    private static final AccuMap.Empty<?, ?> INSTANCE = new EmptyRetainOldestMap<>();

    @SuppressWarnings("unchecked")
    static <K, V> Empty<K, V> instance() {
        return (Empty<K, V>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyRetainOldestMap() {}

    @Override
    public AccuMap<K, V> addAll(Iterator<K> keys, V value) {
        return new SingleIterationRetainOldestMap<>(keys, value);
    }
}

final class SingleIterationRetainOldestMap<K, V>
extends SingleIterationAccumulationMap<K, V, V> {

    SingleIterationRetainOldestMap(Iterator<K> keys, V value) {
        super(keys, value);
    }

    @Override
    public AccuMap.Empty<K, V> empty() {
        return EmptyRetainOldestMap.instance();
    }

    @Override
    protected AccuMap<K, V> emptyComplex() {
        return new RetainOldestMap<>();
    }
}

@SuppressWarnings("serial")
final class RetainOldestMap<K, V>
extends ReductionMap<K, V> {

    @Override
    protected V reduce(V accum, V value) {
        return accum;
    }

    @Override
    public AccuMap.Empty<K, V> empty() {
        return EmptyRetainOldestMap.instance();
    }
}


/* ************* *
 * Retain Latest *
 * ************* */

final class EmptyRetainLatestMap<K, V> extends AccuMap.Empty<K, V> {

    private static final AccuMap.Empty<?, ?> INSTANCE = new EmptyRetainLatestMap<>();

    @SuppressWarnings("unchecked")
    static <K, V> AccuMap.Empty<K, V> instance() {
        return (AccuMap.Empty<K, V>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyRetainLatestMap() {}

    @Override
    public AccuMap<K, V> addAll(Iterator<K> keys, V value) {
        return new SingleIterationRetainLatestMap<>(keys, value);
    }
}

final class SingleIterationRetainLatestMap<K, V>
extends SingleIterationAccumulationMap<K, V, V> {

    SingleIterationRetainLatestMap(Iterator<K> keys, V value) {
        super(keys, value);
    }

    @Override
    public AccuMap.Empty<K, V> empty() {
        return EmptyRetainLatestMap.instance();
    }

    @Override
    protected AccuMap<K, V> emptyComplex() {
        return new RetainLatestMap<>();
    }
}

@SuppressWarnings("serial")
final class RetainLatestMap<K, V>
extends ReductionMap<K, V> {

    @Override
    protected V reduce(V accum, V value) {
        return value;
    }

    @Override
    public AccuMap.Empty<K, V> empty() {
        return EmptyRetainLatestMap.instance();
    }
}


/* ************************ *
 * List change accumulation *
 * ************************ */

final class EmptyListChangeAccuMap<K, E>
extends AccuMap.Empty<K, ListChange<? extends E>> {

    private static final AccuMap.Empty<?, ?> INSTANCE = new EmptyListChangeAccuMap<>();

    @SuppressWarnings("unchecked")
    static <K, E> AccuMap.Empty<K, ListChange<? extends E>> instance() {
        return (AccuMap.Empty<K, ListChange<? extends E>>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyListChangeAccuMap() {}

    @Override
    public AccuMap<K, ListChange<? extends E>> addAll(
            Iterator<K> keys,
            ListChange<? extends E> value) {
        return new SingleIterationListChangeAccuMap<>(keys, value);
    }
}

final class SingleIterationListChangeAccuMap<K, E>
extends SingleIterationAccumulationMap<K, ListChange<? extends E>, ListChangeAccumulator<E>> {

    SingleIterationListChangeAccuMap(
            Iterator<K> keys,
            ListChange<? extends E> value) {
        super(keys, value);
    }

    @Override
    public AccuMap<K, ListChange<? extends E>> empty() {
        return EmptyListChangeAccuMap.instance();
    }

    @Override
    protected AccuMap<K, ListChange<? extends E>> emptyComplex() {
        return new ListChangeAccuMap<>();
    }
}

@SuppressWarnings("serial")
final class ListChangeAccuMap<K, E>
extends AccumulationMap<K, ListChange<? extends E>, ListChangeAccumulator<E>> {

    @Override
    public AccuMap<K, ListChange<? extends E>> empty() {
        return EmptyListChangeAccuMap.instance();
    }

    @Override
    protected AccumulatorSize size(ListChangeAccumulator<E> accum) {
        return accum.isEmpty() ? AccumulatorSize.ZERO : AccumulatorSize.ONE;
    }

    @Override
    protected ListChange<? extends E> head(ListChangeAccumulator<E> accum) {
        return accum.fetch();
    }

    @Override
    protected ListChangeAccumulator<E> tail(ListChangeAccumulator<E> accum) {
        throw new NoSuchElementException();
    }

    @Override
    protected ListChangeAccumulator<E> initialAccumulator(
            ListChange<? extends E> value) {
        ListChangeAccumulator<E> res = new ListChangeAccumulator<>();
        res.add(value);
        return res;
    }

    @Override
    protected ListChangeAccumulator<E> reduce(
            ListChangeAccumulator<E> accum,
            ListChange<? extends E> value) {
        accum.add(value);
        return accum;
    }

}