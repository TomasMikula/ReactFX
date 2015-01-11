package org.reactfx.collection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.Test;

public class ListMapTest {

    @Test
    public void testGet() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        ObsList<Integer> lengths = ObsList.map(strings, String::length);

        assertEquals(Arrays.asList(1, 2, 3), lengths);
    }

    @Test
    public void testChanges() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        ObsList<Integer> lengths = ObsList.map(strings, String::length);

        List<Integer> removed = new ArrayList<>();
        List<Integer> added = new ArrayList<>();
        lengths.observeChanges(ch -> {
            for(ListModification<? extends Integer> mod: ch.getModifications()) {
                removed.addAll(mod.getRemoved());
                added.addAll(mod.getAddedSubList());
            }
        });

        strings.set(1, "4444");

        assertEquals(Arrays.asList(2), removed);
        assertEquals(Arrays.asList(4), added);
    }

    @Test
    public void testLaziness() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        IntegerProperty evaluations = new SimpleIntegerProperty(0);
        ObsList<Integer> lengths = ObsList.map(strings, s -> {
            evaluations.set(evaluations.get() + 1);
            return s.length();
        });

        lengths.observeChanges(ch -> {});
        strings.remove(1);

        assertEquals(0, evaluations.get());
    }

    @Test
    public void testLazinessOnChangeAccumulation() {
        ObservableList<String> strings = FXCollections.observableArrayList("1", "22", "333");
        IntegerProperty evaluations = new SimpleIntegerProperty(0);
        ObsList<Integer> lengths = ObsList.map(strings, s -> {
            evaluations.set(evaluations.get() + 1);
            return s.length();
        });
        SuspendableList<Integer> suspendable = lengths.suspendable();

        suspendable.observeChanges(ch -> {});
        suspendable.suspendWhile(() -> {
            strings.remove(1);
            strings.set(1, "abcd");
        });

        assertEquals(0, evaluations.get());
    }
}
