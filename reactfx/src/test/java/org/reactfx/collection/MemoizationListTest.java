package org.reactfx.collection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;

import org.junit.Test;
import org.reactfx.Counter;

public class MemoizationListTest {

    @Test
    public void test() {
        ObservableList<String> source = new LiveArrayList<>("1", "22", "333");
        IntegerProperty counter = new SimpleIntegerProperty(0);
        MemoizationList<Integer> memoizing = LiveList.map(source, s -> {
            counter.set(counter.get() + 1);
            return s.length();
        }).memoize();
        LiveList<Integer> memoized = memoizing.memoizedItems();
        List<Integer> memoMirror = new ArrayList<>();
        memoized.observeModifications(mod -> {
            memoMirror.subList(mod.getFrom(), mod.getFrom() + mod.getRemovedSize()).clear();
            memoMirror.addAll(mod.getFrom(), mod.getAddedSubList());
        });

        assertEquals(0, memoized.size());

        source.add("4444");
        assertEquals(Collections.emptyList(), memoized);
        assertEquals(0, memoMirror.size());
        assertEquals(0, counter.get());

        memoizing.get(2);
        assertEquals(Arrays.asList(3), memoized);
        assertEquals(Arrays.asList(3), memoMirror);
        assertEquals(1, counter.get());

        counter.set(0);
        memoizing.get(0);
        assertEquals(Arrays.asList(1, 3), memoized);
        assertEquals(Arrays.asList(1, 3), memoMirror);
        assertEquals(1, counter.get());

        counter.set(0);
        source.subList(2, 4).replaceAll(s -> s + s);
        assertEquals(Arrays.asList(1), memoized);
        assertEquals(Arrays.asList(1), memoMirror);
        assertEquals(0, counter.get());

        counter.set(0);
        memoizing.observeModifications(mod -> {
            if(mod.getAddedSize() == 3) { // when three items added
                mod.getAddedSubList().get(0); // force evaluation of the first
                mod.getAddedSubList().get(1); // and second one
                source.remove(0); // and remove the first element from source
            }
        });
        source.remove(1, 4);
        assertEquals(Arrays.asList(1), memoized);
        assertEquals(Arrays.asList(1), memoMirror);
        assertEquals(0, counter.get());
        source.addAll("22", "333", "4444");
        assertEquals(Arrays.asList(2, 3), memoized);
        assertEquals(Arrays.asList(2, 3), memoMirror);
        assertEquals(2, counter.get());

        assertEquals(Arrays.asList(2, 3, 4), memoizing);
        assertEquals(3, counter.get());
        assertEquals(Arrays.asList(2, 3, 4), memoized);
        assertEquals(Arrays.asList(2, 3, 4), memoMirror);
    }

    @Test
    public void testForce() {
        LiveList<Integer> source = new LiveArrayList<>(0, 1, 2, 3, 4, 5, 6);
        MemoizationList<Integer> memoizing = source.memoize();
        LiveList<Integer> memoized = memoizing.memoizedItems();

        memoizing.pin(); // otherwise no memoization takes place

        memoizing.get(3);
        // _ _ _ 3 _ _ _
        assertEquals(Collections.singletonList(3), memoized);

        Counter counter = new Counter();
        memoized.observeChanges(ch -> {
            counter.inc();
            assertEquals(2, ch.getModificationCount());
            ListModification<?> mod1 = ch.getModifications().get(0);
            ListModification<?> mod2 = ch.getModifications().get(1);
            assertEquals(0, mod1.getFrom());
            assertEquals(0, mod1.getRemovedSize());
            assertEquals(Arrays.asList(1, 2), mod1.getAddedSubList());
            assertEquals(3, mod2.getFrom());
            assertEquals(0, mod2.getRemovedSize());
            assertEquals(Arrays.asList(4, 5), mod2.getAddedSubList());
        });

        memoizing.force(1, 6);
        assertEquals(1, counter.get());
    }

    @Test
    public void testForget() {
        LiveList<Integer> source = new LiveArrayList<>(0, 1, 2, 3, 4, 5, 6);
        MemoizationList<Integer> memoizing = source.memoize();
        LiveList<Integer> memoized = memoizing.memoizedItems();

        memoizing.pin(); // otherwise no memoization takes place

        memoizing.force(0, 7);
        assertEquals(7, memoized.size());

        memoizing.forget(2, 4);
        assertEquals(5, memoized.size());

        memoizing.forget(3, 5);
        assertEquals(4, memoized.size());

        Counter counter = new Counter();
        memoized.observeQuasiChanges(ch -> {
            counter.inc();
            assertEquals(1, ch.getModificationCount());
            QuasiListModification<?> mod = ch.getModifications().get(0);
            assertEquals(1, mod.getFrom());
            assertEquals(Arrays.asList(1, 5), mod.getRemoved());
            assertEquals(0, mod.getAddedSize());
        });

        memoizing.forget(1, 6);
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoizationOnlyStartsWhenObsesrved() {
        MemoizationList<Integer> list = new LiveArrayList<>(0, 1, 2, 3, 4, 5, 6).memoize();
        LiveList<Integer> memoized = list.memoizedItems();

        list.get(0);
        assertEquals(0, memoized.size());

        memoized.pin();
        list.get(0);
        assertEquals(1, memoized.size());
    }

    @Test(expected=IllegalStateException.class)
    public void testForceIsNotAllowedWhenUnobserved() {
        MemoizationList<Integer> list = new LiveArrayList<>(0, 1, 2, 3, 4, 5, 6).memoize();

        list.force(2, 4);
    }

    @Test(expected=IllegalStateException.class)
    public void testForgetIsNotAllowedWhenUnobserved() {
        MemoizationList<Integer> list = new LiveArrayList<>(0, 1, 2, 3, 4, 5, 6).memoize();

        list.forget(2, 4);
    }

    @Test
    public void testRecursionWithinForce() {
        LiveList<Integer> src = new LiveArrayList<>(0, 1, 2);
        MemoizationList<Integer> memo1 = src.memoize();
        MemoizationList<Integer> memo2 = memo1.map(Function.identity()).memoize();
        memo1.memoizedItems().sizeProperty().observeInvalidations(__ -> memo2.force(1, 2));

        List<Integer> memo2Mirror = new ArrayList<>();
        memo2.memoizedItems().observeModifications(mod -> {
            memo2Mirror.subList(mod.getFrom(), mod.getFrom() + mod.getRemovedSize()).clear();
            memo2Mirror.addAll(mod.getFrom(), mod.getAddedSubList());
        });

        assertEquals(Collections.emptyList(), memo1.memoizedItems());
        assertEquals(Collections.emptyList(), memo2.memoizedItems());
        assertEquals(Collections.emptyList(), memo2Mirror);

        memo2.force(1, 2); // causes an immediate change in memo1.memoizedItems(),
                           // which recursively calls memo2.force(1, 2).

        assertEquals(Arrays.asList(1), memo2Mirror);
    }
}
