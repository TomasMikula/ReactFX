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
    private static class ChangeObserver<T> {
        public final List<T> removed = new ArrayList<>();
        public final List<T> added = new ArrayList<>();

        public ChangeObserver(LiveList<T> list) {
            list.observeChanges(ch -> {
                for (ListModification<? extends T> mod : ch.getModifications()) {
                    removed.addAll(mod.getRemoved());
                    added.addAll(mod.getAddedSubList());
                }
            });
        }
    }

    @Test
    public void testChanges() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        LiveList<Integer> lengths = LiveList.map(strings, String::length);
        assertEquals(Arrays.asList(1, 2, 3), lengths);

        ChangeObserver<Integer> changes = new ChangeObserver<>(lengths);

        // Set an item
        strings.set(1, "4444");
        assertEquals(Arrays.asList(1, 4, 3), lengths);
        assertEquals(Collections.singletonList(2), changes.removed);
        assertEquals(Collections.singletonList(4), changes.added);

        // Add an item
        strings.add("7777777");
        assertEquals(Arrays.asList(1, 4, 3, 7), lengths);
        assertEquals(Collections.singletonList(2), changes.removed);
        assertEquals(Arrays.asList(4, 7), changes.added);

        // Remove an item
        strings.remove(1);
        assertEquals(Arrays.asList(1, 3, 7), lengths);
        assertEquals(Arrays.asList(2, 4), changes.removed);
        assertEquals(Arrays.asList(4, 7), changes.added);
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
    public void addingAndRemovingMultipleElements() {
        ObservableList<Integer> integers = FXCollections.observableArrayList(3, 4, 5);
        // Live map receives index,item and returns %d-%d index item
        LiveList<Integer> mapped = LiveList.map(integers, (index, item) -> (index + 1) * item);
        assertEquals(Arrays.asList(3, 8, 15), mapped);

        ChangeObserver<Integer> changes = new ChangeObserver<>(mapped);

        // Add an item at position 2
        integers.add(2, 20);
        assertEquals(Arrays.asList(3, 8, 60, 20), mapped);
        assertEquals(Collections.singletonList(60), changes.added);
        assertTrue(changes.removed.isEmpty());

        // Remove an entry to the list and check changes (note that 3-7 becomes 2-7)
        integers.remove(1);
        assertEquals(Arrays.asList(3, 40, 15), mapped);
        assertEquals(Collections.singletonList(60), changes.added);
        assertEquals(Collections.singletonList(8), changes.removed);

        // Add two elements
        integers.addAll(10, 11, 12);
        assertEquals(Arrays.asList(3, 20, 5, 10, 11, 12), integers);
        assertEquals(Arrays.asList(3, 40, 15, 40, 55, 72), mapped);
        assertEquals(Arrays.asList(60, 40, 55, 72), changes.added);
        assertEquals(Collections.singletonList(8), changes.removed);

        // Remove two elements at once
        integers.removeAll(3, 5, 12);
        assertEquals(Arrays.asList(20, 10, 11), integers);
        assertEquals(Arrays.asList(20, 20, 33), mapped);
        assertEquals(Arrays.asList(8, 3, 15, 48), changes.removed);

        // Conditional removal
        integers.removeIf(i -> i % 2 == 0);
        assertEquals(Collections.singletonList(11), integers);
        assertEquals(Collections.singletonList(11), mapped);
        assertEquals(Arrays.asList(8, 3, 15, 48, 20, 20), changes.removed);

        // Remove all
        integers.clear();
        assertTrue(mapped.isEmpty());
        assertEquals(Arrays.asList(60, 40, 55, 72), changes.added);
        assertEquals(Arrays.asList(8, 3, 15, 48, 20, 20, 11), changes.removed);
    }

    @Test
    public void removeMultipleElementsFromIndexList() {
        ObservableList<Integer> integers = FXCollections.observableArrayList(3, 4, 5, 6, 7, 8);
        // Live map receives index,item and returns %d-%d index item
        LiveList<Integer> mapped = LiveList.map(integers, (index, item) -> (index + 1) * item);
        assertEquals(Arrays.asList(3, 8, 15, 24, 35, 48), mapped);
        ChangeObserver<Integer> changes = new ChangeObserver<>(mapped);

        // Remove two elements at once
        integers.removeAll(3, 5, 6, 7);
        assertEquals(Arrays.asList(4, 8), integers);
        assertEquals(Arrays.asList(4, 16), mapped);
        assertTrue(changes.added.isEmpty());
        assertEquals(Arrays.asList(3, 15, 24, 35), changes.removed);
    }

    @Test
    public void removeMultipleElementsFromIndexListUsingPredicate() {
        ObservableList<Integer> integers = FXCollections.observableArrayList(3, 4, 5, 6, 7, 8);
        // Live map receives index,item and returns %d-%d index item
        LiveList<Integer> mapped = LiveList.map(integers, (index, item) -> (index + 1) * item);
        assertEquals(Arrays.asList(3, 8, 15, 24, 35, 48), mapped);
        ChangeObserver<Integer> changes = new ChangeObserver<>(mapped);

        // Remove two elements at once
        integers.removeIf(i -> i % 2 == 0);
        assertEquals(Arrays.asList(3, 5, 7), integers);
        assertEquals(Arrays.asList(3, 10, 21), mapped);
        assertTrue(changes.added.isEmpty());
        assertEquals(Arrays.asList(8, 24, 48), changes.removed);
    }

    @Test
    public void sortIndexedList() {
        ObservableList<Integer> integers = FXCollections.observableArrayList(8, 5, 1, 3, 4);
        LiveList<Integer> mapped = LiveList.map(integers, (index, item) -> (index + 1) * item);
        ChangeObserver<Integer> changes = new ChangeObserver<>(mapped);
        assertEquals(Arrays.asList(8, 10, 3, 12, 20), mapped);

        // Sort the list will remove all and add all
        integers.sort(Integer::compare);
        assertEquals(Arrays.asList(1, 3, 4, 5, 8), integers);
        assertEquals(Arrays.asList(1, 6, 12, 20, 40), mapped);
        assertEquals(Arrays.asList(1, 6, 12, 20, 40), changes.added);
        assertEquals(Arrays.asList(8, 10, 3, 12, 20), changes.removed);
    }

    @Test
    public void setAllContentOfIndexedList() {
        ObservableList<Integer> integers = FXCollections.observableArrayList(8, 5, 1, 3, 4);
        LiveList<Integer> mapped = LiveList.map(integers, (index, item) -> (index + 1) * item);
        ChangeObserver<Integer> changes = new ChangeObserver<>(mapped);
        assertEquals(Arrays.asList(8, 10, 3, 12, 20), mapped);

        // Set all
        integers.setAll(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), integers);
        assertEquals(Arrays.asList(1, 4, 9), mapped);
        assertEquals(Arrays.asList(1, 4, 9), changes.added);
        assertEquals(Arrays.asList(8, 10, 3, 12, 20), changes.removed);
    }

    @Test
    public void removingMultipleElements() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        // Live map receives index,item and returns %d-%d index item
        LiveList<String> lengths = LiveList.map(strings, (index, item) -> String.format("%d-%d", index, item.length()));
        ChangeObserver<String> changes = new ChangeObserver<>(lengths);
        assertLinesMatch(Stream.of("0-1", "1-2", "2-3"), lengths.stream());

        // Remove an entry to the list and check changes (note that 3-7 becomes 2-7)
        strings.removeAll("22", "333");
        assertLinesMatch(Stream.of("1"), strings.stream());
        assertLinesMatch(Stream.of("0-1"), lengths.stream());
        assertTrue(changes.added.isEmpty());
        assertEquals(Arrays.asList("1-2", "2-3"), changes.removed);
    }

    @Test
    public void testIndexedList() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        // Live map receives index,item and returns %d-%d index item
        LiveList<String> lengths = LiveList.map(strings, (index, item) -> String.format("%d-%d", index, item.length()));
        assertLinesMatch(Stream.of("0-1", "1-2", "2-3"), lengths.stream());

        ChangeObserver<String> changes = new ChangeObserver<>(lengths);

        // Set a value in the list and check changes
        strings.set(1, "4444");
        assertLinesMatch(Stream.of("0-1", "1-4", "2-3"), lengths.stream());
        assertEquals(Collections.singletonList("1-4"), changes.added);
        assertEquals(Collections.singletonList("1-2"), changes.removed);

        // Add an entry to the list and check changes
        strings.add("7777777");
        assertLinesMatch(Stream.of("0-1", "1-4", "2-3", "3-7"), lengths.stream());
        assertEquals(Arrays.asList("1-4", "3-7"), changes.added);
        assertEquals(Collections.singletonList("1-2"), changes.removed);

        // Remove an entry to the list and check changes (note that 3-7 becomes 2-7)
        strings.remove(2);
        assertLinesMatch(Stream.of("0-1", "1-4", "2-7"), lengths.stream());
        assertEquals(Arrays.asList("1-4", "3-7"), changes.added);
        assertEquals(Arrays.asList("1-2", "2-3"), changes.removed);
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
