package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Accumulation map.
 *
 * @param <K> key type
 * @param <V> type of individual (non-accumulated) values
 * @param <A> type of accumulated values
 */
public interface AccuMap<K, V, A> {

    static <K, V, A> AccuMap<K, V, A> empty() {
        return EmptyAccuMap.instance();
    }

    boolean isEmpty();
    Tuple2<K, A> peek(AccumulationFacility<V, A> af);
    AccuMap<K, V, A> dropPeeked();
    AccuMap<K, V, A> updatePeeked(A newAccumulatedValue);
    AccuMap<K, V, A> addAll(Iterator<K> keys, V value, AccumulationFacility<V, A> af);
}


class EmptyAccuMap<K, V, A> implements AccuMap<K, V, A> {
    private static final AccuMap<?, ?, ?> INSTANCE = new EmptyAccuMap<>();

    @SuppressWarnings("unchecked")
    static <K, V, A> AccuMap<K, V, A> instance() {
        return (AccuMap<K, V, A>) INSTANCE;
    }

    // private constructor to prevent instantiation
    private EmptyAccuMap() {}

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Tuple2<K, A> peek(AccumulationFacility<V, A> af) {
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
    public AccuMap<K, V, A> addAll(
            Iterator<K> keys, V value,
            AccumulationFacility<V, A> af) {
        return new IteratorBasedAccuMap<>(keys, value);
    }
}


class IteratorBasedAccuMap<K, V, A>
implements AccuMap<K, V, A> {
    private K currentKey = null;
    private A currentAccumulatedValue = null;

    private Iterator<K> it;
    private V value;

    IteratorBasedAccuMap(Iterator<K> keys, V value) {
        this.it = keys;
        this.value = value;
    }

    @Override
    public boolean isEmpty() {
        return currentKey == null && !it.hasNext();
    }

    @Override
    public Tuple2<K, A> peek(AccumulationFacility<V, A> af) {
        if(currentKey == null) {
            currentKey = it.next();
            currentAccumulatedValue = af.initialAccumulator(value);
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
    public AccuMap<K, V, A> addAll(Iterator<K> keys, V value, AccumulationFacility<V, A> af) {
        if(isEmpty()) {
            this.it = keys;
            this.value = value;
            return this;
        } else if(!keys.hasNext()) {
            return this;
        } else {
            HashAccuMap<K, V, A> res = new HashAccuMap<>();
            if(currentKey != null) {
                res.put(currentKey, currentAccumulatedValue);
            }
            return res
                    .addAll(it, this.value, af)
                    .addAll(keys, value, af);
        }
    }

    private final void checkPeeked() {
        if(currentKey == null) {
            throw new NoSuchElementException("No peeked value present. Use peek() first.");
        }
    }
}

@SuppressWarnings("serial")
class HashAccuMap<K, V, A> extends HashMap<K, A> implements AccuMap<K, V, A> {

    @Override
    public Tuple2<K, A> peek(AccumulationFacility<V, A> af) {
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
    public AccuMap<K, V, A> addAll(Iterator<K> keys, V value, AccumulationFacility<V, A> af) {
        while(keys.hasNext()) {
            K key = keys.next();
            if(this.containsKey(key)) {
                A accum = this.get(key);
                accum = af.reduce(accum, value);
                this.put(key, accum);
            } else {
                A accum = af.initialAccumulator(value);
                this.put(key, accum);
            }
        }
        return this;
    }

    private K pickKey() {
        return this.keySet().iterator().next();
    }
}