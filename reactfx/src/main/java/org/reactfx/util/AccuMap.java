package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.reactfx.collection.ListChange;
import org.reactfx.collection.ListChangeAccumulator;
import org.reactfx.collection.ListModificationSequence;
import org.reactfx.collection.ObsList;

/**
 * Accumulation map.
 *
 * @param <K> key type
 * @param <V> type of individual (non-accumulated) values
 * @param <A> type of accumulated values
 */
public interface AccuMap<K, V, A> {

    /**
     * Immutable empty accumulation map.
     * {@link #addAll(Iterator, Object)} must return a new map, because this
     * instance is immutable.
     */
    static abstract class Empty<K, V, A> implements AccuMap<K, V, A> {

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Tuple2<K, A> peek() {
            throw new NoSuchElementException();
        }

        @Override
        public AccuMap<K, V, A> dropPeeked() {
            throw new NoSuchElementException();
        }

        @Override
        public AccuMap<K, V, A> updatePeeked(A newAccumulatedValue) {
            throw new NoSuchElementException();
        }

        @Override
        public AccuMap.Empty<K, V, A> empty() {
            return this;
        }
    }

    static <K, V, A> Empty<K, V, A> emptyAccumulationMap(
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction) {
        return new EmptyGeneralAccuMap<>(
                initialTransformation, reduction);
    }

    static <K, V> Empty<K, V, V> emptyReductionMap(BinaryOperator<V> reduction) {
        return new EmptyGeneralReductionMap<>(reduction);
    }

    static <K, V> Empty<K, V, V> emptyRetainLatestMap() {
        return EmptyRetainLatestMap.instance();
    }

    static <K, V> Empty<K, V, V> emptyRetainOldestMap() {
        return EmptyRetainOldestMap.instance();
    }

    static <K, V> Empty<K, V, V> emptyNonAdditiveMap() {
        return EmptyNonAdditiveMap.instance();
    }

    static <E> Empty<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> emptyListChangeAccumulationMap() {
        return EmptyListChangeAccuMap.instance();
    }


    /* Interface methods */

    boolean isEmpty();
    Tuple2<K, A> peek();
    AccuMap<K, V, A> dropPeeked();
    AccuMap<K, V, A> updatePeeked(A newAccumulatedValue);
    AccuMap<K, V, A> addAll(Iterator<K> keys, V value);
    AccuMap.Empty<K, V, A> empty();
}


abstract class IteratorBasedAccuMap<K, V, A>
implements AccuMap<K, V, A> {
    private K currentKey = null;
    private A currentAccumulatedValue = null;

    private Iterator<K> it;
    private V value;

    IteratorBasedAccuMap(Iterator<K> keys, V value) {
        this.it = keys;
        this.value = value;
    }

    protected abstract A initialAccumulator(V value);
    protected abstract HashAccuMap<K, V, A> emptyHashMap();

    @Override
    public boolean isEmpty() {
        return currentKey == null && !it.hasNext();
    }

    @Override
    public Tuple2<K, A> peek() {
        if(currentKey == null) {
            currentKey = it.next();
            currentAccumulatedValue = initialAccumulator(value);
        }
        return t(currentKey, currentAccumulatedValue);
    }

    @Override
    public AccuMap<K, V, A> dropPeeked() {
        checkPeeked();
        currentKey = null;
        currentAccumulatedValue = null;
        return this;
    }

    @Override
    public AccuMap<K, V, A> updatePeeked(A newAccumulatedValue) {
        checkPeeked();
        currentAccumulatedValue = newAccumulatedValue;
        return this;
    }

    @Override
    public AccuMap<K, V, A> addAll(Iterator<K> keys, V value) {
        if(isEmpty()) {
            this.it = keys;
            this.value = value;
            return this;
        } else if(!keys.hasNext()) {
            return this;
        } else {
            HashAccuMap<K, V, A> res = emptyHashMap();
            if(currentKey != null) {
                res.put(currentKey, currentAccumulatedValue);
            }
            return res
                    .addAll(it, this.value)
                    .addAll(keys, value);
        }
    }

    private final void checkPeeked() {
        if(currentKey == null) {
            throw new NoSuchElementException("No peeked value present. Use peek() first.");
        }
    }
}

@SuppressWarnings("serial")
abstract class HashAccuMap<K, V, A> extends HashMap<K, A> implements AccuMap<K, V, A> {

    protected abstract A initialAccumulator(V value);
    protected abstract A reduce(A accum, V value);

    @Override
    public Tuple2<K, A> peek() {
        K key = pickKey();
        A acc = this.get(key);
        return t(key, acc);
    }

    @Override
    public AccuMap<K, V, A> dropPeeked() {
        K key = pickKey();
        this.remove(key);
        return this;
    }

    @Override
    public AccuMap<K, V, A> updatePeeked(A newAccumulatedValue) {
        K key = pickKey();
        this.put(key, newAccumulatedValue);
        return this;
    }

    @Override
    public AccuMap<K, V, A> addAll(Iterator<K> keys, V value) {
        while(keys.hasNext()) {
            K key = keys.next();
            if(this.containsKey(key)) {
                A accum = this.get(key);
                accum = reduce(accum, value);
                this.put(key, accum);
            } else {
                A accum = initialAccumulator(value);
                this.put(key, accum);
            }
        }
        return this;
    }

    private K pickKey() {
        return this.keySet().iterator().next();
    }
}


/* ************ *
 * Non-additive *
 * ************ */

class EmptyNonAdditiveMap<K, V> extends AccuMap.Empty<K, V, V> {

    private static AccuMap.Empty<?, ?, ?> INSTANCE = new EmptyNonAdditiveMap<>();

    @SuppressWarnings("unchecked")
    static <K, V> AccuMap.Empty<K, V, V> instance() {
        return (AccuMap.Empty<K, V, V>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyNonAdditiveMap() {}

    @Override
    public AccuMap<K, V, V> addAll(Iterator<K> keys, V value) {
        return new IteratorBasedNonAdditiveMap<>(keys, value);
    }
}

class IteratorBasedNonAdditiveMap<K, V> extends IteratorBasedAccuMap<K, V, V> {

    IteratorBasedNonAdditiveMap(Iterator<K> keys, V value) {
        super(keys, value);
    }

    @Override
    public AccuMap.Empty<K, V, V> empty() {
        return EmptyNonAdditiveMap.instance();
    }

    @Override
    protected HashAccuMap<K, V, V> emptyHashMap() {
        throw new IllegalStateException("Cannot accept additional"
                + " entries before removing all previous entries.");
    }

    @Override
    protected V initialAccumulator(V value) {
        return value;
    }
}


/* ******************** *
 * General Accumulation *
 * ******************** */

final class EmptyGeneralAccuMap<K, V, A> extends AccuMap.Empty<K, V, A> {
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    EmptyGeneralAccuMap(
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction) {
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
    }

    @Override
    public AccuMap<K, V, A> addAll(Iterator<K> keys, V value) {
        return new IteratorBasedGeneralAccuMap<>(
                initialTransformation, reduction, keys, value);
    }
}

final class IteratorBasedGeneralAccuMap<K, V, A>
extends IteratorBasedAccuMap<K, V, A> {
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    IteratorBasedGeneralAccuMap(
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction,
            Iterator<K> keys,
            V value) {
        super(keys, value);
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
    }

    @Override
    protected HashAccuMap<K, V, A> emptyHashMap() {
        return new HashGeneralAccuMap<>(initialTransformation, reduction);
    }

    @Override
    public AccuMap.Empty<K, V, A> empty() {
        return new EmptyGeneralAccuMap<>(initialTransformation, reduction);
    }

    @Override
    protected A initialAccumulator(V value) {
        return initialTransformation.apply(value);
    }
}

@SuppressWarnings("serial")
final class HashGeneralAccuMap<K, V, A> extends HashAccuMap<K, V, A> {
    private final Function<? super V, ? extends A> initialTransformation;
    private final BiFunction<? super A, ? super V, ? extends A> reduction;

    HashGeneralAccuMap(
            Function<? super V, ? extends A> initialTransformation,
            BiFunction<? super A, ? super V, ? extends A> reduction) {
        this.initialTransformation = initialTransformation;
        this.reduction = reduction;
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
    public AccuMap.Empty<K, V, A> empty() {
        return new EmptyGeneralAccuMap<>(initialTransformation, reduction);
    }
}


/* ********* *
 * Reduction *
 * ********* */

abstract class IteratorBasedReductionMap<K, V>
extends IteratorBasedAccuMap<K, V, V> {

    IteratorBasedReductionMap(Iterator<K> keys, V value) {
        super(keys, value);
    }

    @Override
    protected final V initialAccumulator(V value) {
        return value;
    }
}

@SuppressWarnings("serial")
abstract class HashReductionMap<K, V> extends HashAccuMap<K, V, V> {

    @Override
    protected final V initialAccumulator(V value) {
        return value;
    }
}


/* ***************** *
 * General Reduction *
 * ***************** */

final class EmptyGeneralReductionMap<K, V> extends AccuMap.Empty<K, V, V> {
    private final BinaryOperator<V> reduction;

    EmptyGeneralReductionMap(BinaryOperator<V> reduction) {
        this.reduction = reduction;
    }

    @Override
    public AccuMap<K, V, V> addAll(Iterator<K> keys, V value) {
        return new IteratorBasedGeneralReductionMap<>(reduction, keys, value);
    }
}

final class IteratorBasedGeneralReductionMap<K, V>
extends IteratorBasedReductionMap<K, V> {
    private final BinaryOperator<V> reduction;

    IteratorBasedGeneralReductionMap(
            BinaryOperator<V> reduction,
            Iterator<K> keys, V value) {
        super(keys, value);
        this.reduction = reduction;
    }

    @Override
    protected HashAccuMap<K, V, V> emptyHashMap() {
        return new HashGeneralReductionMap<K, V>(reduction);
    }

    @Override
    public AccuMap.Empty<K, V, V> empty() {
        return new EmptyGeneralReductionMap<>(reduction);
    }
}

@SuppressWarnings("serial")
final class HashGeneralReductionMap<K, V> extends HashReductionMap<K, V> {
    private final BinaryOperator<V> reduction;

    public HashGeneralReductionMap(BinaryOperator<V> reduction) {
        this.reduction = reduction;
    }

    @Override
    protected V reduce(V accum, V value) {
        return reduction.apply(accum, value);
    }

    @Override
    public AccuMap.Empty<K, V, V> empty() {
        return new EmptyGeneralReductionMap<>(reduction);
    }

}


/* ************* *
 * Retain Oldest *
 * ************* */

final class EmptyRetainOldestMap<K, V> extends AccuMap.Empty<K, V, V> {

    private static final AccuMap.Empty<?, ?, ?> INSTANCE = new EmptyRetainOldestMap<>();

    @SuppressWarnings("unchecked")
    static <K, V> Empty<K, V, V> instance() {
        return (Empty<K, V, V>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyRetainOldestMap() {}

    @Override
    public AccuMap<K, V, V> addAll(Iterator<K> keys, V value) {
        return new IteratorBasedRetainOldestMap<>(keys, value);
    }
}

final class IteratorBasedRetainOldestMap<K, V>
extends IteratorBasedReductionMap<K, V> {

    IteratorBasedRetainOldestMap(Iterator<K> keys, V value) {
        super(keys, value);
    }

    @Override
    public AccuMap.Empty<K, V, V> empty() {
        return EmptyRetainOldestMap.instance();
    }

    @Override
    protected HashAccuMap<K, V, V> emptyHashMap() {
        return new HashRetainOldestMap<>();
    }
}

@SuppressWarnings("serial")
final class HashRetainOldestMap<K, V>
extends HashReductionMap<K, V> {

    @Override
    protected V reduce(V accum, V value) {
        return accum;
    }

    @Override
    public AccuMap.Empty<K, V, V> empty() {
        return EmptyRetainOldestMap.instance();
    }
}


/* ************* *
 * Retain Latest *
 * ************* */

final class EmptyRetainLatestMap<K, V> extends AccuMap.Empty<K, V, V> {

    private static final AccuMap.Empty<?, ?, ?> INSTANCE = new EmptyRetainLatestMap<>();

    @SuppressWarnings("unchecked")
    static <K, V> AccuMap.Empty<K, V, V> instance() {
        return (AccuMap.Empty<K, V, V>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyRetainLatestMap() {}

    @Override
    public AccuMap<K, V, V> addAll(Iterator<K> keys, V value) {
        return new IteratorBasedRetainLatestMap<>(keys, value);
    }
}

final class IteratorBasedRetainLatestMap<K, V>
extends IteratorBasedReductionMap<K, V> {

    IteratorBasedRetainLatestMap(Iterator<K> keys, V value) {
        super(keys, value);
    }

    @Override
    public AccuMap.Empty<K, V, V> empty() {
        return EmptyRetainLatestMap.instance();
    }

    @Override
    protected HashAccuMap<K, V, V> emptyHashMap() {
        return new HashRetainLatestMap<>();
    }
}

@SuppressWarnings("serial")
final class HashRetainLatestMap<K, V>
extends HashReductionMap<K, V> {

    @Override
    protected V reduce(V accum, V value) {
        return value;
    }

    @Override
    public AccuMap.Empty<K, V, V> empty() {
        return EmptyRetainLatestMap.instance();
    }
}


/* ************************ *
 * List change accumulation *
 * ************************ */

final class EmptyListChangeAccuMap<E>
extends AccuMap.Empty<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> {

    private static final AccuMap.Empty<?, ?, ?> INSTANCE = new EmptyListChangeAccuMap<>();

    @SuppressWarnings("unchecked")
    static <E> AccuMap.Empty<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> instance() {
        return (AccuMap.Empty<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyListChangeAccuMap() {}

    @Override
    public AccuMap<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> addAll(
            Iterator<ObsList.Observer<? super E, ?>> keys,
            ListChange<? extends E> value) {
        return new IteratorBasedListChangeAccuMap<>(keys, value);
    }
}

final class IteratorBasedListChangeAccuMap<E>
extends IteratorBasedAccuMap<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> {

    IteratorBasedListChangeAccuMap(
            Iterator<ObsList.Observer<? super E, ?>> keys,
            ListChange<? extends E> value) {
        super(keys, value);
    }

    @Override
    public AccuMap.Empty<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> empty() {
        return EmptyListChangeAccuMap.instance();
    }

    @Override
    protected HashAccuMap<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> emptyHashMap() {
        return new HashListChangeAccuMap<>();
    }

    @Override
    protected ListModificationSequence<E> initialAccumulator(
            ListChange<? extends E> value) {
        return ListChange.safeCast(value);
    }
}

@SuppressWarnings("serial")
final class HashListChangeAccuMap<E>
extends HashAccuMap<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> {

    @Override
    public AccuMap.Empty<ObsList.Observer<? super E, ?>, ListChange<? extends E>, ListModificationSequence<E>> empty() {
        return EmptyListChangeAccuMap.instance();
    }

    @Override
    protected ListModificationSequence<E> initialAccumulator(
            ListChange<? extends E> value) {
        return ListChange.safeCast(value);
    }

    @Override
    protected ListChangeAccumulator<E> reduce(
            ListModificationSequence<E> accum,
            ListChange<? extends E> value) {
        return accum.asListChangeAccumulator().add(value);
    }

}