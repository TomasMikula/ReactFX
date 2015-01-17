package org.reactfx.collection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;

import org.junit.Test;

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
}
