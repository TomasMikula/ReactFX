package org.reactfx.collection;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.reactfx.value.Var;

public class ListMapTest {
    @Test
    public void testChanges() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        LiveList<Integer> lengths = LiveList.map(strings, String::length);
        assertEquals(Arrays.asList(1, 2, 3), lengths);

        List<Integer> removed = new ArrayList<>();
        List<Integer> added = new ArrayList<>();
        lengths.observeChanges(ch -> {
            for(ListModification<? extends Integer> mod: ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
            }
        });

        // Set an item
        strings.set(1, "4444");
        assertEquals(Arrays.asList(1, 4, 3), lengths);
        assertEquals(Collections.singletonList(2), removed);
        assertEquals(Collections.singletonList(4), added);

        // Add an item
        strings.add("7777777");
        assertEquals(Arrays.asList(1, 4, 3, 7), lengths);
        assertEquals(Collections.singletonList(2), removed);
        assertEquals(Arrays.asList(4, 7), added);

        // Remove an item
        strings.remove(1);
        assertEquals(Arrays.asList(1, 3, 7), lengths);
        assertEquals(Arrays.asList(2, 4), removed);
        assertEquals(Arrays.asList(4, 7), added);
    }

    @Test
    public void testLaziness() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        IntegerProperty evaluationsCounter = new SimpleIntegerProperty(0);
        LiveList<Integer> lengths = LiveList.map(strings, elem -> {
            evaluationsCounter.set(evaluationsCounter.get() + 1);
            return elem.length();
        });

        lengths.observeChanges(ch -> {});
        strings.remove(1);

        assertEquals(0, evaluationsCounter.get());

        // Get the first element and the counter has increased
        assertEquals(1, lengths.get(0).intValue());
        assertEquals(1, evaluationsCounter.get());

        // Get the second element, it will evaluate one item
        assertEquals(3, lengths.get(1).intValue());
        assertEquals(2, evaluationsCounter.get());

        // Get again the first, it will reevaluate it
        assertEquals(1, lengths.get(0).intValue());
        assertEquals(3, evaluationsCounter.get());
    }

    @Test
    public void testLazinessOnChangeAccumulation() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        IntegerProperty evaluationsCounter = new SimpleIntegerProperty(0);
        LiveList<Integer> lengths = LiveList.map(strings, elem -> {
            evaluationsCounter.set(evaluationsCounter.get() + 1);
            return elem.length();
        });
        SuspendableList<Integer> suspendable = lengths.suspendable();

        suspendable.observeChanges(ch -> {});
        suspendable.suspendWhile(() -> {
            strings.remove(1);
            strings.set(1, "abcd");
        });

        assertEquals(0, evaluationsCounter.get());
    }

    @Test
    public void testDynamicMap() {
        LiveList<String> strings = new LiveArrayList<>("1", "22", "333");
        Var<Function<String, Integer>> fn = Var.newSimpleVar(String::length);
        SuspendableList<Integer> ints = strings.mapDynamic(fn).suspendable();

        assertEquals(2, ints.get(1).intValue());

        ints.observeChanges(ch -> {
            for(ListModification<?> mod: ch) {
                assertEquals(Arrays.asList(1, 2, 3), mod.getRemoved());
                assertEquals(Arrays.asList(1, 16, 9), mod.getAddedSubList());
            }
        });

        ints.suspendWhile(() -> {
            strings.set(1, "4444");
            fn.setValue(s -> s.length() * s.length());
        });
    }

    @Test
    public void removingMultipleElements() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        // Live map receives index,item and returns %d-%d index item
        LiveList<String> lengths = LiveList.map(strings, (index, item) -> String.format("%d-%d", index, item.length()));
        assertLinesMatch(Stream.of("0-1", "1-2", "2-3"), lengths.stream());

        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        lengths.observeChanges(ch -> {
            for(ListModification<? extends String> mod: ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
            }
        });

        // Remove an entry to the list and check changes (note that 3-7 becomes 2-7)
        strings.removeAll("22", "333");
        assertLinesMatch(Stream.of("0-1"), lengths.stream());
        assertTrue(added.isEmpty());
        assertEquals(Arrays.asList("1-2", "2-3"), removed);
    }

    @Test
    public void testIndexedList() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        // Live map receives index,item and returns %d-%d index item
        LiveList<String> lengths = LiveList.map(strings, (index, item) -> String.format("%d-%d", index, item.length()));
        assertLinesMatch(Stream.of("0-1", "1-2", "2-3"), lengths.stream());

        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        lengths.observeChanges(ch -> {
            for(ListModification<? extends String> mod: ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
            }
        });

        // Set a value in the list and check changes
        strings.set(1, "4444");
        assertLinesMatch(Stream.of("0-1", "1-4", "2-3"), lengths.stream());
        assertEquals(Collections.singletonList("1-4"), added);
        assertEquals(Collections.singletonList("1-2"), removed);

        // Add an entry to the list and check changes
        strings.add("7777777");
        assertLinesMatch(Stream.of("0-1", "1-4", "2-3", "3-7"), lengths.stream());
        assertEquals(Arrays.asList("1-4", "3-7"), added);
        assertEquals(Collections.singletonList("1-2"), removed);

        // Remove an entry to the list and check changes (note that 3-7 becomes 2-7)
        strings.remove(2);
        assertLinesMatch(Stream.of("0-1", "1-4", "2-7"), lengths.stream());
        assertEquals(Arrays.asList("1-4", "3-7"), added);
        assertEquals(Arrays.asList("1-2", "2-3"), removed);
    }

    @Test
    @DisplayName("testLazyIndexedList")
    public void testLazyIndexedList() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        IntegerProperty evaluationsCounter = new SimpleIntegerProperty(0);
        LiveList<String> lengths = LiveList.map(strings, (index, elem) -> {
            evaluationsCounter.set(evaluationsCounter.get() + 1);
            return String.format("%d-%d", index, elem.length());
        });

        lengths.observeChanges(ch -> {});
        strings.remove(1);

        assertEquals(0, evaluationsCounter.get());

        // Get the first element and the counter has increased
        assertEquals("0-1", lengths.get(0));
        assertEquals(1, evaluationsCounter.get());

        // Get the second element, it will evaluate one item
        assertEquals("1-3", lengths.get(1));
        assertEquals(2, evaluationsCounter.get());

        // Get again the first, it will reevaluate it
        assertEquals("0-1", lengths.get(0));
        assertEquals(3, evaluationsCounter.get());
    }

}
